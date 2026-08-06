package com.visium.backend.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarMiPasswordRequest {
	@NotBlank(message = "La contrasena actual es obligatoria")
	private String passwordActual;

	@NotBlank(message = "La nueva contrasena es obligatoria")
	@Size(min = 6, message = "La nueva contrasena debe tener al menos 6 caracteres")
	private String nuevaPassword;
}
