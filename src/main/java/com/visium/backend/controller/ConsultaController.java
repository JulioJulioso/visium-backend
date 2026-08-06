package com.visium.backend.controller;

import com.visium.backend.dto.consulta.CerrarCitaConsultaRequest;
import com.visium.backend.dto.consulta.ConsultaResponse;
import com.visium.backend.service.ConsultaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {

  private final ConsultaService consultaService;

  // TODO: 1. listado de todas las citas
  //           2. filtrar por semana
  // TODO: Profesional queda raro. Hay que ver como se llena la consulta
  @PostMapping("/cerrar-cita")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'RECEPCIONISTA', 'PROFESIONAL')")
  @Operation(
      summary = "Cerrar una cita confirmada y crear la consulta",
      description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, RECEPCIONISTA o PROFESIONAL. "
          + "Convierte una cita CONFIRMADA en ATENDIDA y genera la consulta asociada.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<ConsultaResponse> cerrarCita(
      @Valid @RequestBody CerrarCitaConsultaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.cerrarCita(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'PROFESIONAL')")
  @Operation(
      summary = "Obtener consulta por id",
      description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o PROFESIONAL. "
          + "Devuelve el detalle de una consulta.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<ConsultaResponse> obtener(@PathVariable UUID id) {
    return ResponseEntity.ok(consultaService.obtenerPorId(id));
  }

  @GetMapping("/paciente/{pacienteId}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'PROFESIONAL')")
  @Operation(
      summary = "Historial de consultas de un paciente",
      description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o PROFESIONAL. "
          + "Devuelve todas las consultas realizadas a un paciente.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<ConsultaResponse>> listarPorPaciente(@PathVariable UUID pacienteId) {
    return ResponseEntity.ok(consultaService.listarPorPaciente(pacienteId));
  }
}
