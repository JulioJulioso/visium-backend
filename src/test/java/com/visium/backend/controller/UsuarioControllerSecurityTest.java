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

import com.visium.backend.dto.usuario.CambiarEstadoRequest;
import com.visium.backend.dto.usuario.UsuarioRequest;
import com.visium.backend.dto.usuario.UsuarioResponse;
import com.visium.backend.service.UsuarioService;
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

@SpringJUnitConfig(UsuarioControllerSecurityTest.Config.class)
class UsuarioControllerSecurityTest {

	private static final UUID USUARIO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired private UsuarioController usuarioController;

	@Autowired private UsuarioService usuarioService;

	@BeforeEach
	void setUp() {
		when(usuarioService.listar()).thenReturn(List.of(response()));
		when(usuarioService.obtenerPorId(USUARIO_ID)).thenReturn(response());
		when(usuarioService.crear(any())).thenReturn(response());
		when(usuarioService.editar(any(), any())).thenReturn(response());
		clearInvocations(usuarioService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeListar() {
		assertDoesNotThrow(() -> usuarioController.listar());
		verify(usuarioService).listar();
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeListar() {
		assertDoesNotThrow(() -> usuarioController.listar());
		verify(usuarioService).listar();
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeListar() {
		assertThrows(AccessDeniedException.class, () -> usuarioController.listar());
		verifyNoInteractions(usuarioService);
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaNoPuedeListar() {
		assertThrows(AccessDeniedException.class, () -> usuarioController.listar());
		verifyNoInteractions(usuarioService);
	}

	@Test
	@WithMockUser(roles = "PROFESIONAL")
	void profesionalNoPuedeListar() {
		assertThrows(AccessDeniedException.class, () -> usuarioController.listar());
		verifyNoInteractions(usuarioService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeCrear() {
		assertDoesNotThrow(() -> usuarioController.crear(request()));
		verify(usuarioService).crear(any());
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeCrear() {
		assertDoesNotThrow(() -> usuarioController.crear(request()));
		verify(usuarioService).crear(any());
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeCrear() {
		assertThrows(AccessDeniedException.class, () -> usuarioController.crear(request()));
		verifyNoInteractions(usuarioService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeEditar() {
		assertDoesNotThrow(() -> usuarioController.editar(USUARIO_ID, request()));
		verify(usuarioService).editar(eq(USUARIO_ID), any());
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeEditar() {
		assertDoesNotThrow(() -> usuarioController.editar(USUARIO_ID, request()));
		verify(usuarioService).editar(eq(USUARIO_ID), any());
	}

	@Test
	@WithMockUser(roles = "RECEPCIONISTA")
	void recepcionistaNoPuedeEditar() {
		assertThrows(AccessDeniedException.class, () -> usuarioController.editar(USUARIO_ID, request()));
		verifyNoInteractions(usuarioService);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void superAdminPuedeCambiarEstado() {
		assertDoesNotThrow(() -> usuarioController.cambiarEstado(USUARIO_ID, estadoRequest(true)));
		verify(usuarioService).cambiarEstado(USUARIO_ID, true);
	}

	@Test
	@WithMockUser(roles = "JEFE")
	void jefePuedeCambiarEstado() {
		assertDoesNotThrow(() -> usuarioController.cambiarEstado(USUARIO_ID, estadoRequest(true)));
		verify(usuarioService).cambiarEstado(USUARIO_ID, true);
	}

	@Test
	@WithMockUser(roles = "JEFE_SUCURSAL")
	void jefeSucursalNoPuedeCambiarEstado() {
		assertThrows(AccessDeniedException.class,
				() -> usuarioController.cambiarEstado(USUARIO_ID, estadoRequest(true)));
		verifyNoInteractions(usuarioService);
	}

	private UsuarioRequest request() {
		UsuarioRequest req = new UsuarioRequest();
		req.setEmpresaId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
		req.setNombre("Test");
		req.setApellido("User");
		req.setEmail("test@empresa.com");
		req.setPassword("clave123");
		return req;
	}

	private CambiarEstadoRequest estadoRequest(boolean activo) {
		CambiarEstadoRequest req = new CambiarEstadoRequest();
		req.setActivo(activo);
		return req;
	}

	private UsuarioResponse response() {
		return UsuarioResponse.builder()
				.id(USUARIO_ID)
				.empresaId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
				.nombre("Test")
				.apellido("User")
				.email("test@empresa.com")
				.activo(true)
				.build();
	}

	@Configuration
	@EnableMethodSecurity
	static class Config {

		@Bean
		UsuarioService usuarioService() {
			return mock(UsuarioService.class);
		}

		@Bean
		UsuarioController usuarioController(UsuarioService usuarioService) {
			return new UsuarioController(usuarioService);
		}
	}
}