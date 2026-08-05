package com.visium.backend.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** El codigo y la contrasena no se registran ni se devuelven. */
@Getter
@Setter
@NoArgsConstructor
public class PasswordRecoveryConfirmRequest {
  private String email;
  private String code;
  private String newPassword;
}
