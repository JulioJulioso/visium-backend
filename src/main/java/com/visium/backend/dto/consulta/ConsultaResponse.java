package com.visium.backend.dto.consulta;

import com.visium.backend.enums.EstadoCita;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultaResponse {

  private UUID id;
  private UUID citaId;
  private UUID empresaId;
  private UUID sucursalId;
  private UUID pacienteId;
  private UUID profesionalId;
  private EstadoCita estadoCita;
  private String motivoConsulta;
  private String anamnesis;
  private String examenVisual;
  private String diagnostico;
  private String observaciones;
  private Instant fechaInicio;
  private Instant fechaFin;
  private Instant createdAt;
  private Instant updatedAt;
}
