package com.visium.backend.service;

import com.visium.backend.dto.usuario.UsuarioRequest;
import com.visium.backend.dto.usuario.UsuarioResponse;
import com.visium.backend.entity.Empresa;
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
import com.visium.backend.repository.RolRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.UsuarioEmpresaRolRepository;
import com.visium.backend.repository.UsuarioEmpresaRepository;
import com.visium.backend.repository.UsuarioRepository;
import com.visium.backend.repository.UsuarioSucursalRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion de usuarios del portal: crear, editar, listar y activar/desactivar.
 *
 * <p>Los usuarios nunca se eliminan: el cambio de estado apaga {@code usuarios.activo} y
 * {@code usuarios_empresas.activo} manteniendo las filas. Un usuario inactivo no puede
 * iniciar sesion ({@code UsuarioDetails.isEnabled()}).
 *
 * <p>El alta se crea sin rol asignado; los roles se asignan/editan desde el portal.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

	/** Rolcs que soporta el selector del portal. */
	static final Set<String> ROLES_PERMITIDOS =
			Set.of("SUPER_ADMIN", "JEFE", "JEFE_SUCURSAL", "RECEPCIONISTA", "PROFESIONAL");

	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;
	private final SucursalRepository sucursalRepository;
	private final RolRepository rolRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
	private final UsuarioSucursalRepository usuarioSucursalRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccesoService accesoService;

	@Transactional(readOnly = true)
	public List<UsuarioResponse> listar() {
		return usuarioRepository.findAll().stream()
				.flatMap(usuario -> usuarioEmpresaRepository.findByUsuarioId(usuario.getId()).stream())
				.map(this::toResponse)
				.filter(this::esVisible)
				.toList();
	}

	@Transactional(readOnly = true)
	public UsuarioResponse obtenerPorId(UUID usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
		return usuarioEmpresaRepository.findByUsuarioId(usuario.getId()).stream()
				.map(this::toResponse)
				.filter(this::esVisible)
				.findFirst()
				.orElseThrow(() -> new ForbiddenException("No tienes acceso a ese usuario"));
	}

	@Transactional
	public UsuarioResponse crear(UsuarioRequest request) {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		accesoService.exigirAccesoEmpresa(empresaId);

		validarRolAsignable(request.getRol());

		Empresa empresa = empresaRepository.findById(empresaId)
				.orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));

		usuarioRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(u -> {
			throw new BadRequestException("Ya existe un usuario con el email " + request.getEmail());
		});

		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new BadRequestException("La contrasena es obligatoria");
		}

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

		if (request.getRol() != null) {
			asignarRol(pertenencia, request.getRol());
		}
		if (request.getSucursalIds() != null) {
			asignarSucursales(empresa, pertenencia, request.getSucursalIds());
		}

		return toResponse(pertenencia);
	}

	@Transactional
	public UsuarioResponse editar(UUID usuarioId, UsuarioRequest request) {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		accesoService.exigirAccesoEmpresa(empresaId);

		validarRolAsignable(request.getRol());

		UsuarioEmpresa pertenencia = usuarioEmpresaRepository
				.findByUsuarioIdAndEmpresaId(usuarioId, empresaId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"El usuario no pertenece a la empresa solicitada"));

		Usuario usuario = pertenencia.getUsuario();

		usuarioRepository.findByEmailIgnoreCase(request.getEmail())
				.filter(existente -> !existente.getId().equals(usuarioId))
				.ifPresent(existente -> {
					throw new BadRequestException(
							"Ya existe un usuario con el email " + request.getEmail());
				});

		usuario.setNombre(request.getNombre());
		usuario.setApellido(request.getApellido());
		usuario.setEmail(request.getEmail());
		usuario.setRun(request.getRun());
		usuario.setTelefono(request.getTelefono());
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		}

		// Rol: si viene, reemplaza los anteriores (los usuarios nunca se eliminan).
		if (request.getRol() != null) {
			usuarioEmpresaRolRepository.deleteByUsuarioEmpresaId(pertenencia.getId());
			asignarRol(pertenencia, request.getRol());
		}

		// Sucursales: si vienen, reemplazan las anteriores.
		if (request.getSucursalIds() != null) {
			usuarioSucursalRepository.deleteByUsuarioEmpresaId(pertenencia.getId());
			asignarSucursales(pertenencia.getEmpresa(), pertenencia, request.getSucursalIds());
		}

		usuarioRepository.save(usuario);
		return toResponse(pertenencia);
	}

	/**
	 * Activa/desactiva un usuario dentro de una empresa. Los usuarios nunca se eliminan:
	 * se apagan {@code usuarios.activo} y {@code usuarios_empresas.activo} manteniendo las filas.
	 */
	@Transactional
	public void cambiarEstado(UUID usuarioId, boolean activo) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

		List<UsuarioEmpresa> pertenencias = usuarioEmpresaRepository.findByUsuarioId(usuario.getId());
		if (pertenencias.isEmpty()) {
			throw new ResourceNotFoundException("El usuario no pertenece a ninguna empresa");
		}

		// Solo se tocan las empresas visibles para el operador.
		boolean tocado = false;
		for (UsuarioEmpresa pertenencia : pertenencias) {
			if (!accesoService.puedeAccederEmpresa(pertenencia.getEmpresa().getId())) {
				continue;
			}
			pertenencia.setActivo(activo);
			usuarioEmpresaRepository.save(pertenencia);
			tocado = true;
		}
		if (!tocado) {
			throw new ForbiddenException("No tienes acceso a ese usuario");
		}

		usuario.setActivo(activo);
		usuarioRepository.save(usuario);
	}

	private void validarRolAsignable(String rol) {
		if (rol == null) {
			return;
		}
		if (!ROLES_PERMITIDOS.contains(rol)) {
			throw new BadRequestException("Rol invalido: " + rol);
		}
		if ("SUPER_ADMIN".equals(rol) && !accesoService.esSuperAdmin()) {
			throw new ForbiddenException("Solo SUPER_ADMIN puede asignar el rol SUPER_ADMIN");
		}
	}

	private void asignarRol(UsuarioEmpresa pertenencia, String codigo) {
		Rol rol = rolRepository.findByCodigo(codigo)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Rol " + codigo + " no existe en la base de datos"));
		UsuarioEmpresaRol asignacion = new UsuarioEmpresaRol();
		asignacion.setUsuarioEmpresa(pertenencia);
		asignacion.setRol(rol);
		usuarioEmpresaRolRepository.save(asignacion);
	}

	private void asignarSucursales(Empresa empresa, UsuarioEmpresa pertenencia, List<UUID> sucursalIds) {
		for (UUID sucursalId : sucursalIds) {
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
		}
	}

	private boolean esVisible(UsuarioResponse response) {
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
		return response.getSucursalIds() != null
				&& response.getSucursalIds().stream().anyMatch(permitidas::contains);
	}

	private UsuarioResponse toResponse(UsuarioEmpresa pertenencia) {
		Usuario usuario = pertenencia.getUsuario();

		List<String> roles = usuarioEmpresaRolRepository
				.findByUsuarioEmpresaId(pertenencia.getId()).stream()
				.map(asignacion -> asignacion.getRol().getCodigo())
				.sorted()
				.toList();

		List<UUID> sucursalIds = usuarioSucursalRepository
				.findByUsuarioEmpresaId(pertenencia.getId()).stream()
				.map(UsuarioSucursal::getSucursalId)
				.toList();

		return UsuarioResponse.builder()
				.id(usuario.getId())
				.empresaId(pertenencia.getEmpresa().getId())
				.nombre(usuario.getNombre())
				.apellido(usuario.getApellido())
				.email(usuario.getEmail())
				.run(usuario.getRun())
				.telefono(usuario.getTelefono())
				.activo(usuario.getActivo())
				.roles(roles)
				.sucursalIds(sucursalIds)
				.build();
	}
}