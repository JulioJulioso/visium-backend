package com.visium.backend.dto.cita;

import com.visium.backend.enums.EstadoCita;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CitaResponse {

	private UUID id;
	private UUID empresaId;
	private UUID sucursalId;
	private String sucursalNombre;
	private UUID pacienteId;
	private String pacienteNombre;
	private String pacienteApellido;
	private UUID profesionalId;
	private Instant fechaHoraInicio;
	private Instant fechaHoraFin;
	private EstadoCita estado;
	private String motivo;
	private String observaciones;
	private Instant createdAt;
	private Instant updatedAt;
}
