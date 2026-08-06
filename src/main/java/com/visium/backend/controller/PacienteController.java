package com.visium.backend.controller;

import com.visium.backend.dto.paciente.PacienteRequest;
import com.visium.backend.dto.paciente.PacienteResponse;
import com.visium.backend.service.PacienteService;
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
 * Endpoints de pacientes.
 * Alta/edicion: roles operativos de la optica. Baja: jefatura / plataforma.
 */
@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacienteController {

	private final PacienteService pacienteService;

	@GetMapping
	@Operation(
			summary = "Listar pacientes de una empresa",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve los pacientes de la empresa indicada. "
					+ "Si el usuario tiene varias empresas, enviar la cabecera X-Empresa-Id.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<PacienteResponse>> listar(
			@RequestParam UUID empresaId,
			@RequestParam(required = false) String busqueda) {
		return ResponseEntity.ok(pacienteService.buscarPorEmpresa(empresaId, busqueda));
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Obtener paciente por id",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve el detalle de un paciente especifico.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<PacienteResponse> obtener(@PathVariable UUID id) {
		return ResponseEntity.ok(pacienteService.obtenerPorId(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'JEFE_SUCURSAL', 'RECEPCIONISTA')")
	@Operation(
			summary = "Crear paciente",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, JEFE_SUCURSAL o "
					+ "RECEPCIONISTA. Registra un nuevo paciente en el sistema.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.crear(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'JEFE_SUCURSAL', 'RECEPCIONISTA')")
	@Operation(
			summary = "Actualizar paciente",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, JEFE_SUCURSAL o "
					+ "RECEPCIONISTA. Modifica los datos de un paciente existente.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<PacienteResponse> actualizar(
			@PathVariable UUID id,
			@Valid @RequestBody PacienteRequest request
	) {
		return ResponseEntity.ok(pacienteService.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'JEFE_SUCURSAL')")
	@Operation(
			summary = "Desactivar paciente",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o JEFE_SUCURSAL. "
					+ "Desactiva al paciente (no se elimina fisicamente).")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
		pacienteService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
