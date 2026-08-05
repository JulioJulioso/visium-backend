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

import com.visium.backend.dto.recepcionista.RecepcionistaRequest;
import com.visium.backend.dto.recepcionista.RecepcionistaResponse;
import com.visium.backend.dto.usuario.CambiarEstadoRequest;
import com.visium.backend.service.RecepcionistaService;
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

@SpringJUnitConfig(RecepcionistaControllerSecurityTest.Config.class)
class RecepcionistaControllerSecurityTest {

	private static final UUID RECEPCIONISTA_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

	@Autowired private RecepcionistaController recepcionistaController;

	@Autowired private RecepcionistaService recepcionistaService;

	@BeforeEach
	void setUp() {
		when(recepcionistaService.listar()).thenReturn(List.of(response()));
		when(recepcionistaService.obtenerPorId(RECEPCIONISTA_ID)).thenReturn(response());
		when(recepcionistaService.crear(any())).thenReturn(response());
		when(recepcionistaService.editar(any(), any())).thenReturn(response());
		clearInvocations(recepcionistaService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeListar() {
		assertDoesNotThrow(() -> recepcionistaController.listar());
		verify(recepcionistaService).listar();
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeListar() {
		assertDoesNotThrow(() -> recepcionistaController.listar());
		verify(recepcionistaService).listar();
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeListar() {
		assertThrows(AccessDeniedException.class, () -> recepcionistaController.listar());
		verifyNoInteractions(recepcionistaService);
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaNoPuedeListar() {
		assertThrows(AccessDeniedException.class, () -> recepcionistaController.listar());
		verifyNoInteractions(recepcionistaService);
	}

	@Test
	@WithMockUser(roles = "PROFESIONAL")
	void profesionalNoPuedeListar() {
		assertThrows(AccessDeniedException.class, () -> recepcionistaController.listar());
		verifyNoInteractions(recepcionistaService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeCrear() {
		assertDoesNotThrow(() -> recepcionistaController.crear(request()));
		verify(recepcionistaService).crear(any());
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeCrear() {
		assertDoesNotThrow(() -> recepcionistaController.crear(request()));
		verify(recepcionistaService).crear(any());
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeCrear() {
		assertThrows(AccessDeniedException.class, () -> recepcionistaController.crear(request()));
		verifyNoInteractions(recepcionistaService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeEditar() {
		assertDoesNotThrow(() -> recepcionistaController.editar(RECEPCIONISTA_ID, request()));
		verify(recepcionistaService).editar(eq(RECEPCIONISTA_ID), any());
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeEditar() {
		assertDoesNotThrow(() -> recepcionistaController.editar(RECEPCIONISTA_ID, request()));
		verify(recepcionistaService).editar(eq(RECEPCIONISTA_ID), any());
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaNoPuedeEditar() {
		assertThrows(AccessDeniedException.class,
				() -> recepcionistaController.editar(RECEPCIONISTA_ID, request()));
		verifyNoInteractions(recepcionistaService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeCambiarEstado() {
		assertDoesNotThrow(
				() -> recepcionistaController.cambiarEstado(RECEPCIONISTA_ID, estadoRequest(true)));
		verify(recepcionistaService).cambiarEstado(RECEPCIONISTA_ID, true);
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeCambiarEstado() {
		assertDoesNotThrow(
				() -> recepcionistaController.cambiarEstado(RECEPCIONISTA_ID, estadoRequest(true)));
		verify(recepcionistaService).cambiarEstado(RECEPCIONISTA_ID, true);
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeCambiarEstado() {
		assertThrows(AccessDeniedException.class,
				() -> recepcionistaController.cambiarEstado(RECEPCIONISTA_ID, estadoRequest(true)));
		verifyNoInteractions(recepcionistaService);
	}

	private RecepcionistaRequest request() {
		RecepcionistaRequest req = new RecepcionistaRequest();
		req.setEmpresaId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
		req.setNombre("Test");
		req.setApellido("User");
		req.setEmail("rec@empresa.com");
		req.setPassword("clave123");
		return req;
	}

	private CambiarEstadoRequest estadoRequest(boolean activo) {
		CambiarEstadoRequest req = new CambiarEstadoRequest();
		req.setActivo(activo);
		return req;
	}

	private RecepcionistaResponse response() {
		return RecepcionistaResponse.builder()
				.id(RECEPCIONISTA_ID)
				.empresaId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
				.nombre("Test")
				.apellido("User")
				.email("rec@empresa.com")
				.activo(true)
				.build();
	}

	@Configuration
	@EnableMethodSecurity
	static class Config {

		@Bean
		RecepcionistaService recepcionistaService() {
			return mock(RecepcionistaService.class);
		}

		@Bean
		RecepcionistaController recepcionistaController(RecepcionistaService recepcionistaService) {
			return new RecepcionistaController(recepcionistaService);
		}
	}
}