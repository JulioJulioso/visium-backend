package com.visium.backend.service;

import com.visium.backend.dto.auth.LoginRequest;
import com.visium.backend.dto.auth.LoginResponse;
import com.visium.backend.dto.auth.MeResponse;
import com.visium.backend.dto.auth.PasswordRecoveryConfirmRequest;
import com.visium.backend.dto.auth.PasswordRecoveryRequest;
import com.visium.backend.entity.Usuario;
import com.visium.backend.repository.UsuarioRepository;
import com.visium.backend.security.EmpresaContext;
import com.visium.backend.security.JwtUtil;
import com.visium.backend.security.UsuarioDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Logica de autenticacion: login y datos del usuario actual. */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;
	private final UsuarioRepository usuarioRepository;
	private final PasswordRecoveryService passwordRecoveryService;

	public LoginResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
		);

		UsuarioDetails detalles = (UsuarioDetails) authentication.getPrincipal();
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(detalles.getEmail()).orElseThrow();

		String token = jwtUtil.generarToken(
				detalles.getId(),
				detalles.getEmail(),
				detalles.getRoles(),
				detalles.getEmpresaIds(),
				detalles.getSucursalIds()
		);

		return new LoginResponse(
				token,
				usuario.getId(),
				usuario.getEmail(),
				usuario.getNombre(),
				usuario.getApellido(),
				detalles.getRoles(),
				detalles.getEmpresaIds(),
				detalles.getSucursalIds(),
				sugerirEmpresaActiva(detalles)
		);
	}

	public MeResponse me(UsuarioDetails detalles) {
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(detalles.getEmail()).orElseThrow();

		UUID empresaActiva = EmpresaContext.getEmpresaId();
		if (empresaActiva == null) {
			empresaActiva = sugerirEmpresaActiva(detalles);
		}

		return new MeResponse(
				usuario.getId(),
				usuario.getEmail(),
				usuario.getNombre(),
				usuario.getApellido(),
				detalles.getRoles(),
				detalles.getEmpresaIds(),
				detalles.getSucursalIds(),
				empresaActiva
		);
	}

	/** Nunca autentica ni genera JWT: la recuperacion solo actualiza la credencial. */
	public void requestPasswordRecovery(PasswordRecoveryRequest request) {
		passwordRecoveryService.request(request == null ? null : request.getEmail());
	}

	/** Nunca autentica ni genera JWT: la recuperacion solo actualiza la credencial. */
	public void confirmPasswordRecovery(PasswordRecoveryConfirmRequest request) {
		passwordRecoveryService.confirm(
				request == null ? null : request.getEmail(),
				request == null ? null : request.getCode(),
				request == null ? null : request.getNewPassword());
	}

	private UUID sugerirEmpresaActiva(UsuarioDetails detalles) {
		if (detalles.getEmpresaIds().size() == 1) {
			return detalles.getEmpresaIds().getFirst();
		}
		return null;
	}
}
