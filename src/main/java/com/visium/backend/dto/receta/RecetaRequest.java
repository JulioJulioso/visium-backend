package com.visium.backend.dto.receta;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaRequest {

	@NotNull(message = "El ID de la consulta es obligatorio")
	private UUID consulta;

	private BigDecimal adicion;

	private BigDecimal distanciaPupilar;

	private String indicaciones;

	private String observaciones;

	private List<RecetaDetalleRequest> detalles;
}
