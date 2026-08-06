package com.visium.backend.dto.rol;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RolResponse {
  private Short id;
  private String codigo;
  private String nombre;
}
