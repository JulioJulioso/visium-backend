package com.visium.backend.dto.recepcionista;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Recepcionista sin datos sensibles (nunca se devuelve la contrasena). */
@Getter
@Builder
public class RecepcionistaResponse {

  private UUID id;
  private UUID empresaId;
  private String nombre;
  private String apellido;
  private String email;
  private String run;
  private String telefono;
  private Boolean activo;
  private List<UUID> sucursalIds;
}

