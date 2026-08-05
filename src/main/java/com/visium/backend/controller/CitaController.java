package com.visium.backend.controller;

import com.visium.backend.dto.cita.CitaRequest;
import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

	private static final String ROLES_OPERATIVOS =
			"hasAnyRole('SUPER_ADMIN', 'JEFE', 'JEFE_SUCURSAL', 'RECEPCIONISTA', 'PROFESIONAL')";

	private final CitaService citaService;

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

	@PostMapping
	@PreAuthorize(ROLES_OPERATIVOS)
	@Operation(
			summary = "Crear una cita",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, JEFE_SUCURSAL, RECEPCIONISTA o PROFESIONAL. "
					+ "Crea una cita en estado PENDIENTE. Sucursal, paciente y profesional deben pertenecer "
					+ "a la empresa de la cita.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<CitaResponse> crearCita(@Valid @RequestBody CitaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crearCita(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES_OPERATIVOS)
	@Operation(
			summary = "Modificar una cita",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, JEFE_SUCURSAL, RECEPCIONISTA o PROFESIONAL. "
					+ "Actualiza la cita validando acceso, coherencia con la empresa y transiciones de estado permitidas.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<CitaResponse> modificarCita(
			@PathVariable UUID id, @Valid @RequestBody CitaRequest request) {
		return ResponseEntity.ok(citaService.modificarCita(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(ROLES_OPERATIVOS)
	@Operation(
			summary = "Eliminar una cita",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, JEFE_SUCURSAL, RECEPCIONISTA o PROFESIONAL. "
					+ "Elimina la cita si no tiene consulta registrada.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> eliminarCita(@PathVariable UUID id) {
		citaService.eliminarCita(id);
		return ResponseEntity.noContent().build();
	}
}
