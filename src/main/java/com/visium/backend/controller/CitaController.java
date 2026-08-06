package com.visium.backend.controller;

import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

	private final CitaService citaService;

	@GetMapping
	@Operation(summary = "Listar citas visibles", description = "REQUIERE token JWT. Devuelve las citas de las sucursales autorizadas para el usuario.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<CitaResponse>> listar() {
		return ResponseEntity.ok(citaService.listar());
	}

	@GetMapping("/profesional/{profesionalId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'PROFESIONAL')")
	@Operation(
			summary = "Citas confirmadas de un profesional",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o PROFESIONAL. "
					+ "Devuelve las citas confirmadas del profesional. "
					+ "Los parametros desde/hasta (formato yyyy-MM-dd) son opcionales para filtrar por rango de fechas.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<CitaResponse>> listarCitasConfirmadasPorProfesional(
			@PathVariable UUID profesionalId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return ResponseEntity.ok(
				citaService.listarCitasConfirmadasPorProfesional(profesionalId, desde, hasta));
	}
}
