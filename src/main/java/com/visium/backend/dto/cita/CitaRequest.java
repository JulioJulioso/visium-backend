package com.visium.backend.dto.cita;

import com.visium.backend.enums.EstadoCita;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CitaRequest {
	private UUID empresaId;
	private UUID sucursalId;
	@NotNull private UUID pacienteId;
	@NotNull private UUID profesionalId;
	@NotNull private Instant fechaHoraInicio;
	@NotNull private Instant fechaHoraFin;
	private EstadoCita estado;
	private String motivo;
	private String observaciones;
}
