package com.visium.backend.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.consulta.CerrarCitaConsultaRequest;
import com.visium.backend.dto.consulta.ConsultaResponse;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.service.ConsultaService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ConsultaControllerSecurityTest.Config.class)
class ConsultaControllerSecurityTest {

  private static final UUID CONSULTA_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID CITA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID PACIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Autowired private ConsultaController consultaController;

  @Autowired private ConsultaService consultaService;

  @BeforeEach
  void setUp() {
    when(consultaService.cerrarCita(any())).thenReturn(response());
    when(consultaService.obtenerPorId(CONSULTA_ID)).thenReturn(response());
    when(consultaService.listarPorPaciente(PACIENTE_ID)).thenReturn(List.of(response()));
    clearInvocations(consultaService);
  }

  @Test
  @WithMockUser(roles = "PROFESIONAL")
  void profesionalPuedeCerrarCita() {
    assertDoesNotThrow(() -> consultaController.cerrarCita(request()));
    verify(consultaService).cerrarCita(any());
  }

  @Test
  @WithMockUser(roles = "JEFE")
  void jefePuedeCerrarCita() {
    assertDoesNotThrow(() -> consultaController.cerrarCita(request()));
    verify(consultaService).cerrarCita(any());
  }

  @Test
  @WithMockUser(roles = "SUPER_ADMIN")
  void superAdminPuedeCerrarCita() {
    assertDoesNotThrow(() -> consultaController.cerrarCita(request()));
    verify(consultaService).cerrarCita(any());
  }

  @Test
  @WithMockUser(roles = "JEFE")
  void jefePuedeObtenerHistorialDelPaciente() {
    assertDoesNotThrow(() -> consultaController.listarPorPaciente(PACIENTE_ID));
    verify(consultaService).listarPorPaciente(PACIENTE_ID);
  }

  @Test
  @WithMockUser(roles = "SUPER_ADMIN")
  void superAdminPuedeObtenerConsultaPorId() {
    assertDoesNotThrow(() -> consultaController.obtener(CONSULTA_ID));
    verify(consultaService).obtenerPorId(CONSULTA_ID);
  }

  @Test
  @WithMockUser(roles = "RECEPCIONISTA")
  void recepcionistaPuedeCerrarCita() {
    assertDoesNotThrow(() -> consultaController.cerrarCita(request()));
    verify(consultaService).cerrarCita(any());
  }

  @Test
  @WithMockUser(roles = "RECEPCIONISTA")
  void recepcionistaNoPuedeObtenerConsulta() {
    assertThrows(AccessDeniedException.class, () -> consultaController.obtener(CONSULTA_ID));
    verifyNoInteractions(consultaService);
  }

  @Test
  @WithMockUser(roles = "JEFE_SUCURSAL")
  void jefeSucursalNoPuedeObtenerConsulta() {
    assertThrows(AccessDeniedException.class, () -> consultaController.obtener(CONSULTA_ID));
    verifyNoInteractions(consultaService);
  }

  private CerrarCitaConsultaRequest request() {
    CerrarCitaConsultaRequest request = new CerrarCitaConsultaRequest();
    request.setCitaId(CITA_ID);
    request.setDiagnostico("Control de rutina");
    return request;
  }

  private ConsultaResponse response() {
    return ConsultaResponse.builder()
        .id(CONSULTA_ID)
        .citaId(CITA_ID)
        .pacienteId(PACIENTE_ID)
        .estadoCita(EstadoCita.ATENDIDA)
        .build();
  }

  @Configuration
  @EnableMethodSecurity
  static class Config {

    @Bean
    ConsultaService consultaService() {
      return mock(ConsultaService.class);
    }

    @Bean
    ConsultaController consultaController(ConsultaService consultaService) {
      return new ConsultaController(consultaService);
    }
  }
}
