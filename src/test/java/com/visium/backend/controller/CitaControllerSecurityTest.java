package com.visium.backend.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.cita.CitaRequest;
import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.service.CitaService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(CitaControllerSecurityTest.Config.class)
class CitaControllerSecurityTest {

	private static final UUID PROFESIONAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID CITA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final LocalDate DESDE = LocalDate.of(2026, 8, 3);
	private static final LocalDate HASTA = LocalDate.of(2026, 8, 10);

	@Autowired private CitaController citaController;

	@Autowired private CitaService citaService;

	@BeforeEach
	void setUp() {
		when(citaService.listarCitasConfirmadasPorProfesional(any(), any(), any()))
				.thenReturn(List.of(response()));
		clearInvocations(citaService);
	}

	@Test
	@WithMockUser(roles = "PROFESIONAL")
	void profesionalPuedeListarCitas() {
		assertDoesNotThrow(() -> citaController.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, DESDE, HASTA));
		verify(citaService).listarCitasConfirmadasPorProfesional(PROFESIONAL_ID, DESDE, HASTA);
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeListarCitas() {
		assertDoesNotThrow(() -> citaController.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, DESDE, HASTA));
		verify(citaService).listarCitasConfirmadasPorProfesional(PROFESIONAL_ID, DESDE, HASTA);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeListarCitas() {
		assertDoesNotThrow(() -> citaController.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, DESDE, HASTA));
		verify(citaService).listarCitasConfirmadasPorProfesional(PROFESIONAL_ID, DESDE, HASTA);
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaNoPuedeListarCitas() {
		assertThrows(AccessDeniedException.class, () -> citaController.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, DESDE, HASTA));
		verifyNoInteractions(citaService);
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaPuedeListarCitasDeEmpresa() {
		when(citaService.listarCitas(any(), any(), any())).thenReturn(List.of(response()));

		assertDoesNotThrow(() -> citaController.listarCitas(null, DESDE, HASTA));

		verify(citaService).listarCitas(null, DESDE, HASTA);
	}

	@Test
	@WithMockUser(roles = "PROFESIONAL")
	void profesionalPuedeListarCitasDeEmpresa() {
		when(citaService.listarCitas(any(), any(), any())).thenReturn(List.of(response()));

		assertDoesNotThrow(() -> citaController.listarCitas(EstadoCita.CONFIRMADA, null, null));

		verify(citaService).listarCitas(EstadoCita.CONFIRMADA, null, null);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeListarCitasDeEmpresa() {
		when(citaService.listarCitas(any(), any(), any())).thenReturn(List.of(response()));

		assertDoesNotThrow(() -> citaController.listarCitas(null, null, null));

		verify(citaService).listarCitas(null, null, null);
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeListarCitas() {
		assertThrows(AccessDeniedException.class, () -> citaController.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, DESDE, HASTA));
		verifyNoInteractions(citaService);
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaPuedeCrearCita() {
		when(citaService.crearCita(any(CitaRequest.class))).thenReturn(response());

		assertDoesNotThrow(() -> citaController.crearCita(request()));

		verify(citaService).crearCita(any(CitaRequest.class));
	}

	@Test
	@WithMockUser(roles = "PROFESIONAL")
	void profesionalPuedeCrearCita() {
		when(citaService.crearCita(any(CitaRequest.class))).thenReturn(response());

		assertDoesNotThrow(() -> citaController.crearCita(request()));

		verify(citaService).crearCita(any(CitaRequest.class));
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaPuedeModificarCita() {
		when(citaService.modificarCita(any(), any(CitaRequest.class))).thenReturn(response());

		assertDoesNotThrow(() -> citaController.modificarCita(CITA_ID, request()));

		verify(citaService).modificarCita(eq(CITA_ID), any(CitaRequest.class));
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeEliminarCita() {
		assertDoesNotThrow(() -> citaController.eliminarCita(CITA_ID));

		verify(citaService).eliminarCita(CITA_ID);
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaPuedeEliminarCita() {
		assertDoesNotThrow(() -> citaController.eliminarCita(CITA_ID));

		verify(citaService).eliminarCita(CITA_ID);
	}

	@Test
	void sinAutenticacionNoPuedeCrearCita() {
		// Sin @WithMockUser: @PreAuthorize lanza AuthenticationCredentialsNotFoundException
		assertThrows(AuthenticationCredentialsNotFoundException.class, () -> citaController.crearCita(request()));
		verifyNoInteractions(citaService);
	}

	private CitaRequest request() {
		return CitaRequest.builder()
				.empresaId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
				.sucursalId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
				.pacienteId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
				.profesionalId(PROFESIONAL_ID)
				.fechaHoraInicio(Instant.parse("2026-08-10T09:00:00Z"))
				.fechaHoraFin(Instant.parse("2026-08-10T09:30:00Z"))
				.build();
	}

	private CitaResponse response() {
		return CitaResponse.builder()
				.id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
				.profesionalId(PROFESIONAL_ID)
				.estado(EstadoCita.CONFIRMADA)
				.build();
	}

	@Configuration
	@EnableMethodSecurity
	static class Config {

		@Bean
		CitaService citaService() {
			return mock(CitaService.class);
		}

		@Bean
		CitaController citaController(CitaService citaService) {
			return new CitaController(citaService);
		}
	}
}
