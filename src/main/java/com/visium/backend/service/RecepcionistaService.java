package com.visium.backend.service;

import com.visium.backend.dto.recepcionista.RecepcionistaRequest;
import com.visium.backend.dto.recepcionista.RecepcionistaResponse;
import com.visium.backend.dto.usuario.UsuarioRequest;
import com.visium.backend.dto.usuario.UsuarioResponse;
import com.visium.backend.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recepcionistas = usuarios con el rol RECEPCIONISTA.
 * Reutiliza {@link UsuarioService} forzando el rol; no hay tabla propia.
 */
@Service
@RequiredArgsConstructor
public class RecepcionistaService {

	private static final String ROL_RECEPCIONISTA = "RECEPCIONISTA";

	private final UsuarioService usuarioService;

	@Transactional(readOnly = true)
	public List<RecepcionistaResponse> listar(UUID empresaId, UUID sucursalId) {
		return usuarioService.listar(empresaId, sucursalId).stream()
				.filter(r -> r.getRoles() != null && r.getRoles().contains(ROL_RECEPCIONISTA))
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public RecepcionistaResponse obtenerPorId(UUID id) {
		UsuarioResponse usuario = usuarioService.obtenerPorId(id);
		if (usuario.getRoles() == null || !usuario.getRoles().contains(ROL_RECEPCIONISTA)) {
			throw new ResourceNotFoundException(
					"Recepcionista no encontrado: " + id);
		}
		return toResponse(usuario);
	}

	@Transactional
	public RecepcionistaResponse crear(RecepcionistaRequest request) {
		UsuarioResponse usuario = usuarioService.crear(toUsuarioRequest(request));
		return toResponse(usuario);
	}

	@Transactional
	public RecepcionistaResponse editar(UUID id, RecepcionistaRequest request) {
		UsuarioResponse usuario = usuarioService.editar(id, toUsuarioRequest(request));
		return toResponse(usuario);
	}

	@Transactional
	public void cambiarEstado(UUID id, boolean activo) {
		usuarioService.cambiarEstado(id, activo);
	}

	private UsuarioRequest toUsuarioRequest(RecepcionistaRequest request) {
		UsuarioRequest usuario = new UsuarioRequest();
		usuario.setEmpresaId(request.getEmpresaId());
		usuario.setNombre(request.getNombre());
		usuario.setApellido(request.getApellido());
		usuario.setEmail(request.getEmail());
		usuario.setPassword(request.getPassword());
		usuario.setRun(request.getRun());
		usuario.setTelefono(request.getTelefono());
		usuario.setRol(ROL_RECEPCIONISTA);
		usuario.setSucursalIds(request.getSucursalIds());
		return usuario;
	}

	private RecepcionistaResponse toResponse(UsuarioResponse usuario) {
		return RecepcionistaResponse.builder()
				.id(usuario.getId())
				.empresaId(usuario.getEmpresaId())
				.nombre(usuario.getNombre())
				.apellido(usuario.getApellido())
				.email(usuario.getEmail())
				.run(usuario.getRun())
				.telefono(usuario.getTelefono())
				.activo(usuario.getActivo())
				.sucursalIds(usuario.getSucursalIds())
				.build();
	}
}
