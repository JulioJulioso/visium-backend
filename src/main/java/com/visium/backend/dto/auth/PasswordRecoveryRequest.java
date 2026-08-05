package com.visium.backend.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Solicitud deliberadamente sin validacion HTTP para no revelar detalles del flujo. */
@Getter
@Setter
@NoArgsConstructor
public class PasswordRecoveryRequest {
  private String email;
}
