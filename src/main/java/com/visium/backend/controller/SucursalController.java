package com.visium.backend.controller;

import com.visium.backend.dto.sucursal.SucursalRequest;
import com.visium.backend.dto.sucursal.SucursalResponse;
import com.visium.backend.service.SucursalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de sucursales.
 * Mutaciones: SUPER_ADMIN o JEFE. JEFE_SUCURSAL solo lee las suyas (AccesoService).
 */
@RestController
@RequestMapping("/sucursales")
@RequiredArgsConstructor
public class SucursalController {

	private final SucursalService sucursalService;

	@GetMapping
	@Operation(
			summary = "Listar sucursales de una empresa",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve las sucursales de la empresa indicada.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<SucursalResponse>> listar(
			@RequestParam(required = false) UUID empresaId) {
		return ResponseEntity.ok(sucursalService.listarPorEmpresa(empresaId));
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Obtener sucursal por id",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve el detalle de una sucursal.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<SucursalResponse> obtener(@PathVariable UUID id) {
		return ResponseEntity.ok(sucursalService.obtenerPorId(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Crear sucursal",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Registra una nueva sucursal en una empresa.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<SucursalResponse> crear(@Valid @RequestBody SucursalRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sucursalService.crear(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Actualizar sucursal",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Modifica los datos de una sucursal.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<SucursalResponse> actualizar(
			@PathVariable UUID id,
			@Valid @RequestBody SucursalRequest request
	) {
		return ResponseEntity.ok(sucursalService.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	@Operation(
			summary = "Desactivar sucursal",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN o JEFE. "
					+ "Desactiva la sucursal (no se elimina fisicamente).")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
		sucursalService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
