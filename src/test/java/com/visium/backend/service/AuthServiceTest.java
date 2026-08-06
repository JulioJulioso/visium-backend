package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.auth.CambiarContrasenaRequest;
import com.visium.backend.entity.Usuario;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.repository.UsuarioRepository;
import com.visium.backend.security.JwtUtil;
import com.visium.backend.security.UsuarioDetails;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final UUID USUARIO_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtUtil jwtUtil;
	@Mock private UsuarioRepository usuarioRepository;
	@Mock private PasswordRecoveryService passwordRecoveryService;
	@Mock private PasswordEncoder passwordEncoder;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				authenticationManager, jwtUtil, usuarioRepository, passwordRecoveryService, passwordEncoder);
	}

	@Test
	void cambiarContrasenaValidaLaActualYEncriptaLaNueva() {
		when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
		when(passwordEncoder.matches("actual123", "hashAntiguo")).thenReturn(true);
		when(passwordEncoder.encode("nueva12345")).thenReturn("hashNuevo");

		authService.cambiarContrasena(detalles(), request("actual123", "nueva12345"));

		verify(usuarioRepository).save(any(Usuario.class));
		verify(passwordEncoder).encode("nueva12345");
	}

	@Test
	void cambiarContrasenaConActualIncorrectaFalla() {
		when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
		when(passwordEncoder.matches("incorrecta", "hashAntiguo")).thenReturn(false);

		assertThrows(BadRequestException.class,
				() -> authService.cambiarContrasena(detalles(), request("incorrecta", "nueva12345")));

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void cambiarContrasenaConNuevaMuyCortaFalla() {
		assertThrows(BadRequestException.class,
				() -> authService.cambiarContrasena(detalles(), request("actual123", "corta")));

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void cambiarContrasenaSinCamposFalla() {
		assertThrows(BadRequestException.class, () -> authService.cambiarContrasena(detalles(), request(null, null)));
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(USUARIO_ID);
		usuario.setEmail("test@visium.cl");
		usuario.setPasswordHash("hashAntiguo");
		usuario.setActivo(true);
		return usuario;
	}

	private UsuarioDetails detalles() {
		Usuario usuario = new Usuario();
		usuario.setId(USUARIO_ID);
		usuario.setEmail("test@visium.cl");
		usuario.setPasswordHash("hashAntiguo");
		usuario.setActivo(true);
		return new UsuarioDetails(usuario, List.of("JEFE"), List.of(), List.of());
	}

	private CambiarContrasenaRequest request(String actual, String nueva) {
		CambiarContrasenaRequest request = new CambiarContrasenaRequest();
		request.setPasswordActual(actual);
		request.setNuevaPassword(nueva);
		return request;
	}
}
