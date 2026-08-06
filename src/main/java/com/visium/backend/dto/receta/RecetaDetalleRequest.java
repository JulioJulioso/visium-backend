package com.visium.backend.dto.receta;

import com.visium.backend.enums.Ojo;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class RecetaDetalleRequest {

	@NotNull(message = "El ojo del detalle es obligatorio")
	private Ojo ojo;

	private BigDecimal esfera;

	private BigDecimal cilindro;

	private Short eje;
}
