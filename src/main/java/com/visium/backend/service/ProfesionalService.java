package com.visium.backend.service;

import com.visium.backend.dto.profesional.ProfesionalRequest;
import com.visium.backend.dto.profesional.ProfesionalResponse;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.Rol;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.entity.Usuario;
import com.visium.backend.entity.UsuarioEmpresa;
import com.visium.backend.entity.UsuarioEmpresaRol;
import com.visium.backend.entity.UsuarioSucursal;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.ProfesionalRepository;
import com.visium.backend.repository.RolRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.UsuarioEmpresaRepository;
import com.visium.backend.repository.UsuarioEmpresaRolRepository;
import com.visium.backend.repository.UsuarioRepository;
import com.visium.backend.repository.UsuarioSucursalRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de profesionales segun reglas de negocio: 1) usuario 2) usuarios_empresas 3) rol
 * PROFESIONAL 4) profesionales 5) usuarios_sucursales. Todo en una sola transaccion.
 * Listados filtrados por empresa/sucursal del usuario autenticado.
 */
@Service
@RequiredArgsConstructor
public class ProfesionalService {

	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final RolRepository rolRepository;
	private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
	private final ProfesionalRepository profesionalRepository;
	private final SucursalRepository sucursalRepository;
	private final UsuarioSucursalRepository usuarioSucursalRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccesoService accesoService;

	@Transactional(readOnly = true)
	public List<ProfesionalResponse> listar() {
		return profesionalRepository.findAll().stream()
				.map(this::toResponse)
				.filter(this::esVisible)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProfesionalResponse obtenerPorId(UUID id) {
		Profesional profesional = profesionalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado: " + id));
		ProfesionalResponse response = toResponse(profesional);
		if (!esVisible(response)) {
			throw new ForbiddenException("No tienes acceso a ese profesional");
		}
		return response;
	}

	@Transactional(readOnly = true)
	public List<ProfesionalResponse> listarPorSucursal(UUID sucursalId) {
		Sucursal sucursal = sucursalRepository.findById(sucursalId)
				.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));

		// Verifica que el usuario autenticado tenga permiso en esta sucursal
		accesoService.exigirAccesoSucursal(sucursal.getEmpresa().getId(), sucursalId);

		List<Profesional> profesionales = profesionalRepository.findBySucursalId(sucursalId);

		// 3. Mapear a DTO de respuesta y aplicar filtros de seguridad adicionales
		return profesionales.stream()
				.map(this::toResponse)
				.filter(this::esVisible)
				.toList();
	}

	@Transactional
	public ProfesionalResponse registrar(ProfesionalRequest request) {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		if (!accesoService.esSuperAdmin() && !accesoService.esJefeDeEmpresa()) {
			throw new ForbiddenException("Solo JEFE o SUPER_ADMIN pueden registrar profesionales");
		}

		Empresa empresa = empresaRepository.findById(empresaId)
				.orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));

		usuarioRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(u -> {
			throw new BadRequestException("Ya existe un usuario con el email " + request.getEmail());
		});



		Usuario usuario = new Usuario();
		usuario.setNombre(request.getNombre());
		usuario.setApellido(request.getApellido());
		usuario.setEmail(request.getEmail());
		usuario.setRun(request.getRun());
		usuario.setTelefono(request.getTelefono());
		usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		usuario.setActivo(true);
		usuario = usuarioRepository.save(usuario);

		UsuarioEmpresa pertenencia = new UsuarioEmpresa();
		pertenencia.setUsuario(usuario);
		pertenencia.setEmpresa(empresa);
		pertenencia.setActivo(true);
		pertenencia = usuarioEmpresaRepository.save(pertenencia);

		Rol rolProfesional = rolRepository.findByCodigo("PROFESIONAL")
				.orElseThrow(() -> new ResourceNotFoundException(
						"Rol PROFESIONAL no existe en la base de datos"));
		UsuarioEmpresaRol asignacionRol = new UsuarioEmpresaRol();
		asignacionRol.setUsuarioEmpresa(pertenencia);
		asignacionRol.setRol(rolProfesional);
		usuarioEmpresaRolRepository.save(asignacionRol);

		Profesional profesional = new Profesional();
		profesional.setUsuario(usuario);
		profesional.setEspecialidad(request.getEspecialidad());
		profesional.setActivo(true);
		profesional = profesionalRepository.save(profesional);

		List<UUID> sucursalIds = new ArrayList<>();
		for (UUID sucursalId : request.getSucursalIds()) {
			Sucursal sucursal = sucursalRepository.findById(sucursalId)
					.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));

			if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
				throw new BadRequestException("La sucursal " + sucursalId + " no pertenece a la empresa");
			}
			accesoService.exigirAccesoSucursal(empresa.getId(), sucursal.getId());

			UsuarioSucursal usuarioSucursal = new UsuarioSucursal();
			usuarioSucursal.setUsuarioEmpresaId(pertenencia.getId());
			usuarioSucursal.setSucursalId(sucursal.getId());
			usuarioSucursal.setEmpresaId(empresa.getId());
			usuarioSucursalRepository.save(usuarioSucursal);
			sucursalIds.add(sucursal.getId());
		}

		return ProfesionalResponse.builder()
				.id(profesional.getId())
				.usuarioId(usuario.getId())
				.empresaId(empresa.getId())
				.nombre(usuario.getNombre())
				.apellido(usuario.getApellido())
				.email(usuario.getEmail())
				.especialidad(profesional.getEspecialidad())
				.activo(profesional.getActivo())
				.sucursalIds(sucursalIds)
				.build();
	}

	private boolean esVisible(ProfesionalResponse response) {
		if (response.getEmpresaId() == null) {
			return accesoService.esSuperAdmin();
		}
		if (!accesoService.puedeAccederEmpresa(response.getEmpresaId())) {
			return false;
		}
		List<UUID> permitidas = accesoService.sucursalIdsVisiblesEnEmpresa();
		if (permitidas.isEmpty()) {
			return true;
		}
		// Debe compartir al menos una sucursal con el usuario
		return response.getSucursalIds() != null
				&& response.getSucursalIds().stream().anyMatch(permitidas::contains);
	}

	private ProfesionalResponse toResponse(Profesional profesional) {
		Usuario usuario = profesional.getUsuario();
		List<UsuarioEmpresa> pertenencias = usuarioEmpresaRepository.findByUsuarioId(usuario.getId());
		UUID empresaId = pertenencias.isEmpty() ? null : pertenencias.getFirst().getEmpresa().getId();

		List<UUID> sucursalIds = new ArrayList<>();
		if (!pertenencias.isEmpty()) {
			sucursalIds = usuarioSucursalRepository
					.findByUsuarioEmpresaId(pertenencias.getFirst().getId()).stream()
					.map(UsuarioSucursal::getSucursalId)
					.toList();
		}

		return ProfesionalResponse.builder()
				.id(profesional.getId())
				.usuarioId(usuario.getId())
				.empresaId(empresaId)
				.nombre(usuario.getNombre())
				.apellido(usuario.getApellido())
				.email(usuario.getEmail())
				.especialidad(profesional.getEspecialidad())
				.activo(profesional.getActivo())
				.sucursalIds(sucursalIds)
				.build();
	}
}
