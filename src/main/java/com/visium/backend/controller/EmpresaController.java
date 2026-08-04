package com.visium.backend.controller;

import com.visium.backend.dto.empresa.EmpresaRequest;
import com.visium.backend.dto.empresa.EmpresaResponse;
import com.visium.backend.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de empresas.
 * Crear / desactivar: SUPER_ADMIN. Actualizar: SUPER_ADMIN o JEFE (AccesoService limita alcance).
 */
@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

	private final EmpresaService empresaService;

	@GetMapping("/")
	@Operation(
			summary = "Listar empresas",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve las empresas visibles para el usuario.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<EmpresaResponse>> listar() {
		return ResponseEntity.ok(empresaService.listar());
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Obtener empresa por id",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve el detalle de una empresa.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<EmpresaResponse> obtener(@PathVariable UUID id) {
		return ResponseEntity.ok(empresaService.obtenerPorId(id));
	}

	@PostMapping
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(
			summary = "Crear empresa",
			description = "REQUIERE token JWT. Rol: SUPER_ADMIN. "
					+ "Registra una nueva empresa en el sistema.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody EmpresaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.crear(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Actualizar empresa",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Modifica los datos de una empresa.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<EmpresaResponse> actualizar(
			@PathVariable UUID id, @Valid @RequestBody EmpresaRequest request) {
		return ResponseEntity.ok(empresaService.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(
			summary = "Desactivar empresa",
			description = "REQUIERE token JWT. Rol: SUPER_ADMIN. "
					+ "Desactiva la empresa (no se elimina fisicamente).")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
		empresaService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
