package com.visium.backend.dto.dashboard;

import com.visium.backend.dto.cita.CitaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Resumen del dashboard de un usuario para la empresa/sucursal activa.
 * <p>
 * Los totales y las proximas citas se calculan desde la base de datos,
 * filtrados por la empresa activa y las sucursales autorizadas del usuario.
 */
@Getter
@Builder
@Schema(description = "Resumen del dashboard: totales y proximas citas")
public class DashboardResumenResponse {

	@Schema(description = "Empresa sobre la que se calculo el resumen")
	private UUID empresaId;

	@Schema(description = "Cantidad de pacientes de la empresa", example = "42")
	private long totalPacientes;

	@Schema(description = "Cantidad de citas confirmadas de hoy (sucursales autorizadas)", example = "3")
	private long citasConfirmadasHoy;

	@Schema(description = "Proximas 5 citas confirmadas (desde ahora, ordenadas por fecha)")
	private List<CitaResponse> proximasCitas;
}
