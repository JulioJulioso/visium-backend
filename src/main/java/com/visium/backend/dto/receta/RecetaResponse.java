package com.visium.backend.dto.receta;

import com.visium.backend.enums.Ojo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecetaResponse {

	private UUID id;
	private UUID consultaId;
	private UUID pacienteId;
	private LocalDate fechaEmision;
	private LocalDate vigenciaHasta;
	private BigDecimal adicion;
	private BigDecimal distanciaPupilar;
	private String indicaciones;
	private String observaciones;
	private List<RecetaDetalleResponse> detalles;

	@Getter
	@Builder
	public static class RecetaDetalleResponse {
		private Ojo ojo;
		private BigDecimal esfera;
		private BigDecimal cilindro;
		private Short eje;
	}
}
