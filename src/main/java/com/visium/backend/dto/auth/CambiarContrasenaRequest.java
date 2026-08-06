package com.visium.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Cambio de contrasena del usuario autenticado. */
@Getter
@Setter
public class CambiarContrasenaRequest {

	@NotBlank(message = "La contrasena actual es obligatoria")
	private String passwordActual;

	@NotBlank(message = "La nueva contrasena es obligatoria")
	private String nuevaPassword;
}
