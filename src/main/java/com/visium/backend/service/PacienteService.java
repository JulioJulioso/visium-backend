package com.visium.backend.service;

import com.visium.backend.dto.paciente.PacienteHistorialResponse;
import com.visium.backend.dto.paciente.PacientePageResponse;
import com.visium.backend.dto.paciente.PacienteRequest;
import com.visium.backend.dto.paciente.PacienteResponse;
import com.visium.backend.entity.Consulta;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.FichaClinica;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.enums.TipoHistorial;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.mapper.PacienteMapper;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.FichaClinicaRepository;
import com.visium.backend.repository.PacienteRepository;
import com.visium.backend.repository.RecetaOpticaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PacienteService {

	private final PacienteRepository pacienteRepository;
	private final EmpresaRepository empresaRepository;
	private final FichaClinicaRepository fichaClinicaRepository;
	private final ConsultaRepository consultaRepository;
	private final RecetaOpticaRepository recetaOpticaRepository;
	private final PacienteMapper pacienteMapper;
	private final AccesoService accesoService;

	private static final int TAMANIO_MAXIMO_PAGINA = 100;

	@Transactional(readOnly = true)
	public List<PacienteResponse> listarPorEmpresa(UUID empresaId) {
		UUID empresa = accesoService.resolverEmpresaObjetivo(empresaId);
		return pacienteRepository.findByEmpresaId(empresa).stream()
				.map(pacienteMapper::toResponse)
				.toList();
	}

	/**
	 * Listado paginado con busqueda por texto (nombre, apellido, documento, email o telefono).
	 * El texto null o vacio devuelve todos los pacientes de la empresa.
	 */
	@Transactional(readOnly = true)
	public PacientePageResponse listarPaginado(UUID empresaId, String texto, int page, int size) {
		UUID empresa = accesoService.resolverEmpresaObjetivo(empresaId);
		int pagina = Math.max(page, 0);
		int tamano = Math.min(Math.max(size, 1), TAMANIO_MAXIMO_PAGINA);

		Pageable pageable =
				PageRequest.of(
						pagina,
						tamano,
						Sort.by("apellido").ascending().and(Sort.by("nombre").ascending()));

		Page<Paciente> resultado = pacienteRepository.buscarPorEmpresa(empresa, texto, pageable);

		return PacientePageResponse.builder()
				.content(resultado.getContent().stream().map(pacienteMapper::toResponse).toList())
				.page(resultado.getNumber())
				.size(resultado.getSize())
				.totalElements(resultado.getTotalElements())
				.totalPages(resultado.getTotalPages())
				.build();
	}

	/**
	 * Historial del paciente: consultas y recetas en una sola lista cronologica descendente.
	 * Solo incluye registros de sucursales autorizadas para el usuario.
	 */
	@Transactional(readOnly = true)
	public List<PacienteHistorialResponse> historial(UUID pacienteId) {
		Paciente paciente = buscarOFallar(pacienteId);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		List<UUID> sucursalesVisibles = accesoService.sucursalIdsVisiblesEnEmpresa();

		return java.util.stream.Stream.concat(
						consultaRepository
								.findByCitaPacienteIdOrderByCreatedAtDesc(pacienteId)
								.stream()
								.filter(c -> sucursalVisible(c, sucursalesVisibles))
								.map(this::consultaAItem),
						recetaOpticaRepository
								.findHistorialByPacienteId(pacienteId)
								.stream()
								.filter(r -> sucursalVisible(r, sucursalesVisibles))
								.map(this::recetaAItem))
				.sorted(Comparator.comparing(PacienteHistorialResponse::getFecha).reversed())
				.toList();
	}

	private boolean sucursalVisible(Consulta consulta, List<UUID> sucursalesVisibles) {
		return sucursalesVisibles.isEmpty()
				|| sucursalesVisibles.contains(consulta.getCita().getSucursal().getId());
	}

	private boolean sucursalVisible(RecetaOptica receta, List<UUID> sucursalesVisibles) {
		return sucursalesVisibles.isEmpty()
				|| sucursalesVisibles.contains(
						receta.getConsulta().getCita().getSucursal().getId());
	}

	private PacienteHistorialResponse consultaAItem(Consulta consulta) {
		return PacienteHistorialResponse.builder()
				.id(consulta.getId())
				.tipo(TipoHistorial.CONSULTA)
				.fecha(consulta.getFechaInicio() != null ? consulta.getFechaInicio() : consulta.getCreatedAt())
				.detalle(consulta.getDiagnostico())
				.referenciaId(consulta.getCita().getId())
				.profesionalNombre(nombreProfesional(consulta))
				.build();
	}

	private PacienteHistorialResponse recetaAItem(RecetaOptica receta) {
		return PacienteHistorialResponse.builder()
				.id(receta.getId())
				.tipo(TipoHistorial.RECETA)
				.fecha(receta.getCreatedAt())
				.detalle(receta.getIndicaciones())
				.referenciaId(receta.getConsulta().getId())
				.profesionalNombre(nombreProfesional(receta))
				.build();
	}

	private String nombreProfesional(Consulta consulta) {
		var profesional = consulta.getCita().getProfesional();
		var usuario = profesional.getUsuario();
		return usuario.getNombre() + " " + usuario.getApellido();
	}

	private String nombreProfesional(RecetaOptica receta) {
		return nombreProfesional(receta.getConsulta());
	}

	@Transactional(readOnly = true)
	public PacienteResponse obtenerPorId(UUID id) {
		Paciente paciente = buscarOFallar(id);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		return pacienteMapper.toResponse(paciente);
	}

	@Transactional
	public PacienteResponse crear(PacienteRequest request) {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		Empresa empresa = verificarEmpresa(empresaId);
		Paciente paciente = pacienteMapper.toEntity(request, empresa);
		if (paciente.getActivo() == null) {
			paciente.setActivo(true);
		}
		paciente = pacienteRepository.save(paciente);

		FichaClinica ficha = new FichaClinica();
		ficha.setPaciente(paciente);
		fichaClinicaRepository.save(ficha);

		return pacienteMapper.toResponse(paciente);
	}

	@Transactional
	public PacienteResponse actualizar(UUID id, PacienteRequest request) {
		Paciente paciente = buscarOFallar(id);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		Empresa empresa = verificarEmpresa(empresaId);
		paciente.setEmpresa(empresa);
		pacienteMapper.aplicar(paciente, request);
		return pacienteMapper.toResponse(pacienteRepository.save(paciente));
	}

	@Transactional
	public void desactivar(UUID id) {
		Paciente paciente = buscarOFallar(id);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		paciente.setActivo(false);
		pacienteRepository.save(paciente);
	}

	private Empresa verificarEmpresa(UUID empresaId) {
		return empresaRepository.findById(empresaId)
				.orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));
	}

	private Paciente buscarOFallar(UUID id) {
		return pacienteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + id));
	}
}
