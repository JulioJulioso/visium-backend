package com.visium.backend.dto.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de PATCH /usuarios/{id}/estado y /recepcionistas/{id}/estado.
 * true = activo, false = desactivado (nunca se elimina al usuario).
 */
@Getter
@Setter
public class CambiarEstadoRequest {

	@NotNull(message = "El campo activo es obligatorio")
	private Boolean activo;
}