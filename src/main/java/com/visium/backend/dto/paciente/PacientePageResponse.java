package com.visium.backend.dto.paciente;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta paginada del listado de pacientes.
 * <p>
 * Estructura clasica tipo Spring Page pero en DTO propio:
 * content (pagina actual), page, size, totalElements y totalPages.
 */
@Getter
@Builder
@Schema(description = "Pagina de pacientes con metadatos de paginacion")
public class PacientePageResponse {

	@Schema(description = "Pacientes de la pagina actual")
	private List<PacienteResponse> content;

	@Schema(description = "Numero de pagina devuelto (desde 0)", example = "0")
	private int page;

	@Schema(description = "Cantidad de registros por pagina", example = "20")
	private int size;

	@Schema(description = "Total de pacientes que coinciden con la busqueda", example = "120")
	private long totalElements;

	@Schema(description = "Cantidad total de paginas", example = "6")
	private int totalPages;
}
