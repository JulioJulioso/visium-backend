package com.visium.backend.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.service.CitaService;
import java.time.LocalDate;
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

@SpringJUnitConfig(CitaControllerSecurityTest.Config.class)
class CitaControllerSecurityTest {

	private static final UUID PROFESIONAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
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
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeListarCitas() {
		assertThrows(AccessDeniedException.class, () -> citaController.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, DESDE, HASTA));
		verifyNoInteractions(citaService);
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
