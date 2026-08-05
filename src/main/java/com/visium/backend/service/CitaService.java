package com.visium.backend.service;

import com.visium.backend.dto.cita.CitaRequest;
import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.entity.UsuarioEmpresa;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.mapper.CitaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.PacienteRepository;
import com.visium.backend.repository.ProfesionalRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.UsuarioEmpresaRepository;
import com.visium.backend.security.UsuarioDetails;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CitaService {

	private final CitaRepository citaRepository;
	private final ProfesionalRepository profesionalRepository;
	private final SucursalRepository sucursalRepository;
	private final PacienteRepository pacienteRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final CitaMapper citaMapper;
	private final AccesoService accesoService;

	@Transactional(readOnly = true)
	public List<CitaResponse> listarCitasConfirmadasPorProfesional(
			UUID profesionalId, LocalDate desde, LocalDate hasta) {
		if (desde != null && hasta != null && hasta.isBefore(desde)) {
			throw new BadRequestException("El rango de fechas es invalido: hasta no puede ser antes de desde");
		}

		Profesional profesional = profesionalRepository.findById(profesionalId)
				.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado: " + profesionalId));

		UsuarioDetails usuario = accesoService.usuarioActual();
		validarProfesional(profesionalId, usuario);
		validarEmpresaDeProfesional(profesional, usuario);

		List<Cita> citas;
		if (desde == null && hasta == null) {
			citas = citaRepository.findByProfesionalIdAndEstadoOrderByFechaHoraInicioAsc(
					profesionalId, EstadoCita.CONFIRMADA);
		} else {
			LocalDate inicioReal = desde != null ? desde : LocalDate.of(1970, 1, 1);
			LocalDate finReal = hasta != null ? hasta : LocalDate.of(9999, 12, 31);
			Instant inicio = inicioReal.atStartOfDay().toInstant(ZoneOffset.UTC);
			Instant fin = finReal.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
			citas = citaRepository.findByProfesionalIdAndFechaHoraInicioBetweenAndEstado(
					profesionalId, inicio, fin, EstadoCita.CONFIRMADA);
		}

		return citas.stream()
				.filter(this::tieneAccesoACita)
				.map(citaMapper::toResponse)
				.toList();
	}

	@Transactional
	public CitaResponse crearCita(CitaRequest request) {
		accesoService.exigirAccesoSucursal(request.getEmpresaId(), request.getSucursalId());

		Cita nuevaCita = citaMapper.toEntity(request);

		// Asignación de entidades relacionadas
		Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
				.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + request.getSucursalId()));
		Paciente paciente = pacienteRepository.findById(request.getPacienteId())
				.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + request.getPacienteId()));
		Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
				.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado: " + request.getProfesionalId()));

		// Obtener la entidad UsuarioEmpresa del usuario que está creando la cita
		UsuarioDetails usuarioActual = accesoService.usuarioActual();
		List<UsuarioEmpresa> pertenencias = usuarioEmpresaRepository.findByUsuarioId(usuarioActual.getId());

		UsuarioEmpresa creador = pertenencias.stream()
				.filter(ue -> ue.getEmpresa().getId().equals(request.getEmpresaId()))
				.findFirst()
				.orElseThrow(() -> new ForbiddenException("El usuario actual no pertenece a la empresa de la cita"));

		nuevaCita.setSucursal(sucursal);
		nuevaCita.setPaciente(paciente);
		nuevaCita.setProfesional(profesional);
		nuevaCita.setCreadaPor(creador);

		if (nuevaCita.getEstado() == null) {
			nuevaCita.setEstado(EstadoCita.PENDIENTE);
		}

		Cita citaGuardada = citaRepository.save(nuevaCita);
		return citaMapper.toResponse(citaGuardada);
	}

	@Transactional
	public CitaResponse modificarCita(UUID citaId, CitaRequest request) {
		Cita citaExistente = citaRepository.findById(citaId)
				.orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));

		if (citaExistente.getSucursal() != null) {
			accesoService.exigirAccesoSucursal(citaExistente.getEmpresaId(), citaExistente.getSucursal().getId());
		}

		// Si cambia la sucursal, se validan permisos y actualiza la relación
		if (request.getSucursalId() != null &&
				(citaExistente.getSucursal() == null || !citaExistente.getSucursal().getId().equals(request.getSucursalId()))) {
			accesoService.exigirAccesoSucursal(request.getEmpresaId(), request.getSucursalId());
			Sucursal nuevaSucursal = sucursalRepository.findById(request.getSucursalId())
					.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + request.getSucursalId()));
			citaExistente.setSucursal(nuevaSucursal);
		}

		// Actualizar Paciente si cambió
		if (request.getPacienteId() != null &&
				(citaExistente.getPaciente() == null || !citaExistente.getPaciente().getId().equals(request.getPacienteId()))) {
			Paciente nuevoPaciente = pacienteRepository.findById(request.getPacienteId())
					.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + request.getPacienteId()));
			citaExistente.setPaciente(nuevoPaciente);
		}

		// Actualizar Profesional si cambió
		if (request.getProfesionalId() != null &&
				(citaExistente.getProfesional() == null || !citaExistente.getProfesional().getId().equals(request.getProfesionalId()))) {
			Profesional nuevoProfesional = profesionalRepository.findById(request.getProfesionalId())
					.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado: " + request.getProfesionalId()));
			citaExistente.setProfesional(nuevoProfesional);
		}

		citaMapper.updateEntityFromRequest(request, citaExistente);

		Cita citaActualizada = citaRepository.save(citaExistente);
		return citaMapper.toResponse(citaActualizada);
	}

	@Transactional
	public void eliminarCita(UUID citaId) {
		Cita cita = citaRepository.findById(citaId)
				.orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));

		if (cita.getSucursal() != null) {
			accesoService.exigirAccesoSucursal(cita.getEmpresaId(), cita.getSucursal().getId());
		}

		citaRepository.delete(cita);
	}


	private boolean tieneAccesoACita(Cita cita) {
		try {
			if (cita.getSucursal() != null) {
				accesoService.exigirAccesoSucursal(cita.getEmpresaId(), cita.getSucursal().getId());
			}
			return true;
		} catch (ForbiddenException ex) {
			return false;
		}
	}

	private void validarProfesional(UUID profesionalId, UsuarioDetails usuario) {
		if (accesoService.esSuperAdmin() || accesoService.esJefeDeEmpresa()) {
			return;
		}
		if (!usuario.getId().equals(profesionalId)) {
			throw new ForbiddenException("Solo puedes consultar tus propias citas");
		}
	}

	private void validarEmpresaDeProfesional(Profesional profesional, UsuarioDetails usuario) {
		if (accesoService.esSuperAdmin()) {
			return;
		}
		List<UsuarioEmpresa> pertenencias = usuarioEmpresaRepository.findByUsuarioId(profesional.getUsuario().getId());
		if (pertenencias.isEmpty()) {
			throw new ForbiddenException("El profesional no tiene empresa asignada");
		}
		for (UsuarioEmpresa pertenencia : pertenencias) {
			accesoService.exigirAccesoEmpresa(pertenencia.getEmpresa().getId());
		}
	}
}