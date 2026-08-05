package com.visium.backend.dto.consulta;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultaRequest {

  @NotNull(message = "La cita es obligatoria")
  private UUID citaId;

  private String motivoConsulta;

  private String anamnesis;

  private String examenVisual;

  private String diagnostico;

  private String observaciones;
}
