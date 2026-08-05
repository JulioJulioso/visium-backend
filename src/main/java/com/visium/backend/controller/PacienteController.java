package com.visium.backend.controller;

import com.visium.backend.dto.paciente.PacienteHistorialResponse;
import com.visium.backend.dto.paciente.PacientePageResponse;
import com.visium.backend.dto.paciente.PacienteRequest;
import com.visium.backend.dto.paciente.PacienteResponse;
import com.visium.backend.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
			summary = "Listar pacientes de una empresa (paginado y con busqueda)",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve una pagina de pacientes de la empresa indicada. "
					+ "texto (opcional) busca por nombre, apellido, documento, email o telefono. "
					+ "page y size controlan la paginacion. "
					+ "Si el usuario tiene varias empresas, enviar la cabecera X-Empresa-Id.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<PacientePageResponse> listar(
			@Parameter(description = "Id de la empresa cuyos pacientes se listan", required = true)
					@RequestParam UUID empresaId,
			@Parameter(
							description = "Texto de busqueda (opcional): filtra por nombre, apellido, "
									+ "documento, email o telefono. Vacio = sin filtro",
							example = "Juan")
					@RequestParam(required = false)
					String texto,
			@Parameter(description = "Numero de pagina (desde 0)", example = "0")
					@RequestParam(defaultValue = "0")
					int page,
			@Parameter(description = "Tamano de pagina (1-100, por defecto 20)", example = "20")
					@RequestParam(defaultValue = "20")
					int size) {
		return ResponseEntity.ok(pacienteService.listarPaginado(empresaId, texto, page, size));
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Obtener paciente por id",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve el detalle de un paciente especifico.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<PacienteResponse> obtener(
			@Parameter(description = "Id del paciente", required = true) @PathVariable UUID id) {
		return ResponseEntity.ok(pacienteService.obtenerPorId(id));
	}

	@GetMapping("/{id}/historial")
	@Operation(
			summary = "Historial del paciente (consultas y recetas)",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve consultas y recetas del paciente en una sola lista "
					+ "cronologica descendente. Solo incluye sucursales autorizadas del usuario.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<PacienteHistorialResponse>> historial(
			@Parameter(description = "Id del paciente", required = true) @PathVariable UUID id) {
		return ResponseEntity.ok(pacienteService.historial(id));
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
			summary = "Desactivar paciente (baja logica)",
			description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o JEFE_SUCURSAL. "
					+ "Desactiva al paciente (no se elimina fisicamente: se marca activo=false).")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
		pacienteService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
