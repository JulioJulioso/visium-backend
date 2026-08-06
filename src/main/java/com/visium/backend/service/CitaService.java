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
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.PacienteRepository;
import com.visium.backend.repository.ProfesionalRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.UsuarioEmpresaRepository;
import com.visium.backend.security.UsuarioDetails;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CitaService {

	/** Transiciones de estado permitidas (máquina de estados). ATENDIDA y NO_ASISTIO son terminales. */
	private static final Map<EstadoCita, Set<EstadoCita>> TRANSICIONES = Map.of(
			EstadoCita.PENDIENTE, Set.of(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA),
			EstadoCita.CONFIRMADA, Set.of(EstadoCita.PENDIENTE, EstadoCita.CANCELADA),
			EstadoCita.CANCELADA, Set.of(EstadoCita.PENDIENTE));

	private final CitaRepository citaRepository;
	private final ProfesionalRepository profesionalRepository;
	private final SucursalRepository sucursalRepository;
	private final PacienteRepository pacienteRepository;
	private final ConsultaRepository consultaRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final CitaMapper citaMapper;
	private final AccesoService accesoService;

	/**
	 * Listado de citas de la empresa activa (X-Empresa-Id), filtradas por estado y rango de fechas,
	 * restringidas a las sucursales autorizadas del usuario.
	 */
	@Transactional(readOnly = true)
	public List<CitaResponse> listarCitas(EstadoCita estado, LocalDate desde, LocalDate hasta) {
		if (desde != null && hasta != null && hasta.isBefore(desde)) {
			throw new BadRequestException("El rango de fechas es invalido: hasta no puede ser antes de desde");
		}

		UUID empresaId = accesoService.resolverEmpresaObjetivo(null);
		List<UUID> sucursalesVisibles = accesoService.sucursalIdsVisiblesEnEmpresa();

		LocalDate inicioReal = desde != null ? desde : LocalDate.of(1970, 1, 1);
		LocalDate finReal = hasta != null ? hasta : LocalDate.of(9999, 12, 31);
		Instant inicio = inicioReal.atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant fin = finReal.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

		return citaRepository.listarEnRango(empresaId, sucursalesVisibles, estado, inicio, fin)
				.stream()
				.filter(this::tieneAccesoACita)
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

	@Transactional
	public CitaResponse crearCita(CitaRequest request) {
		if (request == null) {
			throw new BadRequestException("El cuerpo de la solicitud es obligatorio");
		}
		if (request.getEmpresaId() == null || request.getSucursalId() == null
				|| request.getPacienteId() == null || request.getProfesionalId() == null
				|| request.getFechaHoraInicio() == null || request.getFechaHoraFin() == null) {
			throw new BadRequestException("Faltan datos obligatorios para crear la cita");
		}
		validarRangoHorario(request.getFechaHoraInicio(), request.getFechaHoraFin());
		if (request.getEstado() != null && request.getEstado() != EstadoCita.PENDIENTE) {
			throw new BadRequestException("Una cita nueva solo puede crearse como PENDIENTE");
		}

		accesoService.exigirAccesoSucursal(request.getEmpresaId(), request.getSucursalId());

		Cita nuevaCita = citaMapper.toEntity(request);

		// Asignación de entidades relacionadas, validando que todo pertenezca a la misma empresa
		Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
				.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + request.getSucursalId()));
		validarSucursalDeEmpresa(sucursal, request.getEmpresaId());

		Paciente paciente = pacienteRepository.findById(request.getPacienteId())
				.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + request.getPacienteId()));
		validarPacienteDeEmpresa(paciente, request.getEmpresaId());

		Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
				.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado: " + request.getProfesionalId()));
		validarProfesionalDeEmpresa(profesional, request.getEmpresaId());

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
		nuevaCita.setEstado(EstadoCita.PENDIENTE);

		Cita citaGuardada = citaRepository.save(nuevaCita);
		return citaMapper.toResponse(citaGuardada);
	}

	@Transactional
	public CitaResponse modificarCita(UUID citaId, CitaRequest request) {
		if (request == null) {
			throw new BadRequestException("El cuerpo de la solicitud es obligatorio");
		}
		Cita citaExistente = citaRepository.findById(citaId)
				.orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));

		if (citaExistente.getSucursal() != null) {
			accesoService.exigirAccesoSucursal(citaExistente.getEmpresaId(), citaExistente.getSucursal().getId());
		}

		exigirCitaModificable(citaExistente);

		// Empresa final de la cita tras la modificación
		UUID empresaNueva = request.getEmpresaId() != null
				? request.getEmpresaId()
				: citaExistente.getEmpresaId();

		// Si cambia la empresa, se valida acceso a la empresa nueva
		if (!empresaNueva.equals(citaExistente.getEmpresaId())) {
			accesoService.exigirAccesoEmpresa(empresaNueva);
		}

		// Si cambia la sucursal, se validan permisos y coherencia con la empresa final
		if (request.getSucursalId() != null &&
				(citaExistente.getSucursal() == null || !citaExistente.getSucursal().getId().equals(request.getSucursalId()))) {
			accesoService.exigirAccesoSucursal(empresaNueva, request.getSucursalId());
			Sucursal nuevaSucursal = sucursalRepository.findById(request.getSucursalId())
					.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + request.getSucursalId()));
			validarSucursalDeEmpresa(nuevaSucursal, empresaNueva);
			citaExistente.setSucursal(nuevaSucursal);
		} else if (citaExistente.getSucursal() != null
				&& !citaExistente.getSucursal().getEmpresa().getId().equals(empresaNueva)) {
			// Coherencia empresa/sucursal: no puede quedar la sucursal de otra empresa
			throw new BadRequestException("La sucursal de la cita no pertenece a la empresa indicada");
		}

		// Actualizar Paciente si cambió, validando que pertenezca a la empresa final
		if (request.getPacienteId() != null &&
				(citaExistente.getPaciente() == null || !citaExistente.getPaciente().getId().equals(request.getPacienteId()))) {
			Paciente nuevoPaciente = pacienteRepository.findById(request.getPacienteId())
					.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + request.getPacienteId()));
			validarPacienteDeEmpresa(nuevoPaciente, empresaNueva);
			citaExistente.setPaciente(nuevoPaciente);
		}

		// Actualizar Profesional si cambió, validando que pertenezca a la empresa final
		if (request.getProfesionalId() != null &&
				(citaExistente.getProfesional() == null || !citaExistente.getProfesional().getId().equals(request.getProfesionalId()))) {
			Profesional nuevoProfesional = profesionalRepository.findById(request.getProfesionalId())
					.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado: " + request.getProfesionalId()));
			validarProfesionalDeEmpresa(nuevoProfesional, empresaNueva);
			citaExistente.setProfesional(nuevoProfesional);
		}

		// Coherencia de fechas con los valores finales
		validarRangoHorario(
				request.getFechaHoraInicio() != null ? request.getFechaHoraInicio() : citaExistente.getFechaHoraInicio(),
				request.getFechaHoraFin() != null ? request.getFechaHoraFin() : citaExistente.getFechaHoraFin());

		// Máquina de estados
		if (request.getEstado() != null && request.getEstado() != citaExistente.getEstado()) {
			Set<EstadoCita> permitidas = TRANSICIONES.get(citaExistente.getEstado());
			if (permitidas == null || !permitidas.contains(request.getEstado())) {
				throw new BadRequestException("Transicion de estado invalida: "
						+ citaExistente.getEstado() + " -> " + request.getEstado());
			}
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

		if (consultaRepository.findByCitaId(citaId).isPresent()) {
			throw new BadRequestException("No se puede eliminar una cita que ya tiene consulta registrada");
		}

		citaRepository.delete(cita);
	}

	private void exigirCitaModificable(Cita cita) {
		if (cita.getEstado() == EstadoCita.ATENDIDA || cita.getEstado() == EstadoCita.NO_ASISTIO) {
			throw new BadRequestException("No se puede modificar una cita " + cita.getEstado());
		}
	}

	private void validarRangoHorario(Instant inicio, Instant fin) {
		if (!fin.isAfter(inicio)) {
			throw new BadRequestException("La fecha de fin debe ser posterior a la de inicio");
		}
	}

	private void validarSucursalDeEmpresa(Sucursal sucursal, UUID empresaId) {
		if (!sucursal.getEmpresa().getId().equals(empresaId)) {
			throw new BadRequestException("La sucursal no pertenece a la empresa indicada");
		}
	}

	private void validarPacienteDeEmpresa(Paciente paciente, UUID empresaId) {
		if (!paciente.getEmpresa().getId().equals(empresaId)) {
			throw new BadRequestException("El paciente no pertenece a la empresa indicada");
		}
	}

	private void validarProfesionalDeEmpresa(Profesional profesional, UUID empresaId) {
		List<UsuarioEmpresa> pertenencias = usuarioEmpresaRepository.findByUsuarioId(profesional.getUsuario().getId());
		boolean pertenece = pertenencias.stream()
				.anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId));
		if (!pertenece) {
			throw new BadRequestException("El profesional no pertenece a la empresa indicada");
		}
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
