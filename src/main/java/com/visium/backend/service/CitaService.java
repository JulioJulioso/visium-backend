package com.visium.backend.service;

import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.dto.cita.CitaRequest;
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
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CitaService {

	private final CitaRepository citaRepository;
	private final PacienteRepository pacienteRepository;
	private final ProfesionalRepository profesionalRepository;
	private final SucursalRepository sucursalRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final CitaMapper citaMapper;
	private final AccesoService accesoService;

	@Transactional
	public CitaResponse crear(CitaRequest request) {
		if (!request.getFechaHoraFin().isAfter(request.getFechaHoraInicio())) {
			throw new BadRequestException("La hora de término debe ser posterior a la de inicio");
		}
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		UUID sucursalId = accesoService.resolverSucursalObjetivo(empresaId, request.getSucursalId());
		Sucursal sucursal = sucursalRepository.findById(sucursalId)
				.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
		if (!empresaId.equals(sucursal.getEmpresa().getId())) throw new BadRequestException("La sucursal no pertenece a la empresa activa");
		Paciente paciente = pacienteRepository.findById(request.getPacienteId())
				.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
		Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
				.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
		if (!empresaId.equals(paciente.getEmpresa().getId()) || !empresaId.equals(profesional.getEmpresaId()) || !sucursalId.equals(profesional.getSucursalId())) {
			throw new BadRequestException("Paciente o profesional fuera del contexto activo");
		}
		UsuarioEmpresa creador = usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(accesoService.usuarioActual().getId(), empresaId)
				.orElseThrow(() -> new ForbiddenException("No tienes una pertenencia activa en la empresa"));
		Cita cita = new Cita();
		cita.setEmpresaId(empresaId); cita.setSucursal(sucursal); cita.setPaciente(paciente); cita.setProfesional(profesional); cita.setCreadaPor(creador);
		cita.setFechaHoraInicio(request.getFechaHoraInicio()); cita.setFechaHoraFin(request.getFechaHoraFin());
		cita.setEstado(request.getEstado() == null ? EstadoCita.PENDIENTE : request.getEstado()); cita.setMotivo(request.getMotivo()); cita.setObservaciones(request.getObservaciones());
		return citaMapper.toResponse(citaRepository.save(cita));
	}

	/** Lista las citas visibles en las sucursales autorizadas del usuario. */
	@Transactional(readOnly = true)
	public List<CitaResponse> listar(UUID empresaIdSolicitada, UUID sucursalId) {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(empresaIdSolicitada);
		if (sucursalId != null) accesoService.exigirAccesoSucursal(empresaId, sucursalId);
		return citaRepository.findByEmpresaId(empresaId).stream()
				.filter(cita -> sucursalId == null || sucursalId.equals(cita.getSucursal().getId()))
				.filter(this::tieneAccesoACita)
				.sorted(Comparator.comparing(Cita::getFechaHoraInicio))
				.map(citaMapper::toResponse)
				.toList();
	}

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

	private boolean tieneAccesoACita(Cita cita) {
		try {
			accesoService.exigirAccesoSucursal(cita.getEmpresaId(), cita.getSucursal().getId());
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
