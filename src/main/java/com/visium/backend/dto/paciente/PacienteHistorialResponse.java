package com.visium.backend.dto.paciente;

import com.visium.backend.enums.TipoHistorial;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Item del historial de un paciente: una consulta o una receta optica.
 * <p>
 * El historial combina consultas y recetas en una sola lista cronologica
 * (tipo CONSULTA o RECETA), ordenada por fecha descendente.
 */
@Getter
@Builder
@Schema(description = "Item del historial del paciente: una consulta o una receta optica")
public class PacienteHistorialResponse {

	@Schema(description = "Id de la consulta o de la receta, segun el tipo")
	private UUID id;

	@Schema(description = "Tipo de item: CONSULTA o RECETA", example = "CONSULTA")
	private TipoHistorial tipo;

	@Schema(description = "Fecha de la consulta o de la receta (ISO-8601)")
	private Instant fecha;

	@Schema(description = "Diagnostico (consulta) o indicaciones (receta), si existe")
	private String detalle;

	@Schema(
			description = "Id de la cita asociada (si es consulta) o de la consulta asociada (si es receta)")
	private UUID referenciaId;

	@Schema(description = "Nombre completo del profesional que atendio")
	private String profesionalNombre;
}
