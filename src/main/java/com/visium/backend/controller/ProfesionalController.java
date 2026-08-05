package com.visium.backend.controller;

import com.visium.backend.dto.profesional.ProfesionalRequest;
import com.visium.backend.dto.profesional.ProfesionalResponse;
import com.visium.backend.service.ProfesionalService;
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
	public ResponseEntity<List<ProfesionalResponse>> listar() {
		return ResponseEntity.ok(profesionalService.listar());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProfesionalResponse> obtener(@PathVariable UUID id) {
		return ResponseEntity.ok(profesionalService.obtenerPorId(id));
	}
	@GetMapping("/sucursal/{sucursalId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'JEFE_SUCURSAL')")
	public ResponseEntity<List<ProfesionalResponse>> listarPorSucursal(@PathVariable UUID sucursalId) {
		return ResponseEntity.ok(profesionalService.listarPorSucursal(sucursalId));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
	public ResponseEntity<ProfesionalResponse> registrar(
			@Valid @RequestBody ProfesionalRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(profesionalService.registrar(request));
	}
}
