package com.visium.backend.controller;

import com.visium.backend.dto.recepcionista.RecepcionistaRequest;
import com.visium.backend.dto.recepcionista.RecepcionistaResponse;
import com.visium.backend.dto.usuario.CambiarEstadoRequest;
import com.visium.backend.service.RecepcionistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion de recepcionistas (usuarios con rol RECEPCIONISTA).
 * Solo SUPER_ADMIN y JEFE operan este modulo.
 */
@RestController
@RequestMapping("/recepcionistas")
@RequiredArgsConstructor
public class RecepcionistaController {

	private final RecepcionistaService recepcionistaService;

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Listar recepcionistas",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Devuelve todos los usuarios con rol RECEPCIONISTA.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<RecepcionistaResponse>> listar() {
		return ResponseEntity.ok(recepcionistaService.listar());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Obtener recepcionista por id",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Devuelve el detalle de un recepcionista especifico.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<RecepcionistaResponse> obtener(@PathVariable UUID id) {
		return ResponseEntity.ok(recepcionistaService.obtenerPorId(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Crear recepcionista",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Crea un usuario con rol RECEPCIONISTA asignado automaticamente.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<RecepcionistaResponse> crear(
			@Valid @RequestBody RecepcionistaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(recepcionistaService.crear(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Editar recepcionista",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Actualiza los datos del recepcionista.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<RecepcionistaResponse> editar(
			@PathVariable UUID id, @Valid @RequestBody RecepcionistaRequest request) {
		return ResponseEntity.ok(recepcionistaService.editar(id, request));
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Activar o desactivar recepcionista",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "activo=true lo activa, activo=false lo desactiva. "
					+ "Nunca se elimina al usuario, solo se desactiva.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> cambiarEstado(
			@PathVariable UUID id, @Valid @RequestBody CambiarEstadoRequest request) {
		recepcionistaService.cambiarEstado(id, Boolean.TRUE.equals(request.getActivo()));
		return ResponseEntity.noContent().build();
	}
}