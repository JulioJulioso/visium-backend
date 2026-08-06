package com.visium.backend.controller;

import com.visium.backend.dto.profesional.ProfesionalRequest;
import com.visium.backend.dto.profesional.ProfesionalResponse;
import com.visium.backend.service.ProfesionalService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de profesionales.
 * Registro: SUPER_ADMIN o JEFE. Listado filtrado por AccesoService.
 */
@RestController
@RequestMapping("/profesionales")
@RequiredArgsConstructor
public class ProfesionalController {

	private final ProfesionalService profesionalService;

	@GetMapping
	@Operation(
			summary = "Listar profesionales",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve todos los profesionales registrados.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<ProfesionalResponse>> listar() {
		return ResponseEntity.ok(profesionalService.listar());
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Obtener profesional por id",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve el detalle de un profesional.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ProfesionalResponse> obtener(@PathVariable UUID id) {
		return ResponseEntity.ok(profesionalService.obtenerPorId(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Registrar profesional",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Registra un nuevo profesional en el sistema.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ProfesionalResponse> registrar(
			@Valid @RequestBody ProfesionalRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(profesionalService.registrar(request));
	}

	@PutMapping("/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	public ResponseEntity<ProfesionalResponse> editar(@PathVariable UUID id, @Valid @RequestBody ProfesionalRequest request) { return ResponseEntity.ok(profesionalService.editar(id, request)); }
	@PatchMapping("/{id}/estado") @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	public ResponseEntity<Void> cambiarEstado(@PathVariable UUID id, @RequestBody com.visium.backend.dto.usuario.CambiarEstadoRequest request) { profesionalService.cambiarEstado(id, Boolean.TRUE.equals(request.getActivo())); return ResponseEntity.noContent().build(); }
}
