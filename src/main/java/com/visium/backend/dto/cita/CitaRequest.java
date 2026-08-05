package com.visium.backend.dto.cita;

import com.visium.backend.enums.EstadoCita;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
public class CitaRequest {

    @NotNull(message = "El ID de la empresa es obligatorio")
    private UUID empresaId;

    @NotNull(message = "El ID de la sucursal es obligatorio")
    private UUID sucursalId;

    @NotNull(message = "El ID del paciente es obligatorio")
    private UUID pacienteId;

    @NotNull(message = "El ID del profesional es obligatorio")
    private UUID profesionalId;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    private Instant fechaHoraInicio;

    @NotNull(message = "La fecha y hora de fin es obligatoria")
    private Instant fechaHoraFin;

    private EstadoCita estado;

    private String motivo;

    private String observaciones;
}
