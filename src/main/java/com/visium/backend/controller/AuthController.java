package com.visium.backend.controller;

import com.visium.backend.dto.auth.CambiarContrasenaRequest;
import com.visium.backend.dto.auth.LoginRequest;
import com.visium.backend.dto.auth.LoginResponse;
import com.visium.backend.dto.auth.MeResponse;
import com.visium.backend.dto.auth.PasswordRecoveryConfirmRequest;
import com.visium.backend.dto.auth.PasswordRecoveryRequest;
import com.visium.backend.security.UsuarioDetails;
import com.visium.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Endpoints de autenticacion.
 * POST /auth/login  -> publico
 * GET  /auth/me     -> requiere token; opcional header X-Empresa-Id
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@Operation(
			summary = "Iniciar sesion",
			description = "Endpoint PUBLICO (no requiere token JWT). Valida email y contraseña, "
					+ "y devuelve el token JWT necesario para consumir el resto de la API "
					+ "(enviarlo en la cabecera Authorization: Bearer <token>).")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	@Operation(
			summary = "Obtener datos del usuario autenticado",
			description = "REQUIERE token JWT (Authorization: Bearer <token>). Devuelve la "
					+ "informacion del usuario asociado al token actual.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UsuarioDetails detalles) {
		return ResponseEntity.ok(authService.me(detalles));
	}

	@PutMapping("/me/password")
	@Operation(
			summary = "Cambiar la contrasena del usuario autenticado",
			description = "REQUIERE token JWT. Valida la contrasena actual y la reemplaza por la nueva.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Void> cambiarContrasena(
			@AuthenticationPrincipal UsuarioDetails detalles,
			@Valid @RequestBody CambiarContrasenaRequest request) {
		authService.cambiarContrasena(detalles, request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/password-recovery")
	public ResponseEntity<Map<String, String>> requestPasswordRecovery(@RequestBody PasswordRecoveryRequest request) {
		authService.requestPasswordRecovery(request);
		return ResponseEntity.ok(recoveryResponse());
	}

	@PostMapping("/password-recovery/confirm")
	public ResponseEntity<Map<String, String>> confirmPasswordRecovery(
			@RequestBody PasswordRecoveryConfirmRequest request) {
		authService.confirmPasswordRecovery(request);
		return ResponseEntity.ok(recoveryResponse());
	}

	private Map<String, String> recoveryResponse() {
		return Map.of("message", "Si la solicitud es valida, recibiras instrucciones para continuar.");
	}
}
