package com.visium.backend.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tipos de item que puede contener el historial de un paciente.
 */
@Schema(description = "Tipo de item del historial: consulta o receta optica")
public enum TipoHistorial {
	CONSULTA,
	RECETA
}
