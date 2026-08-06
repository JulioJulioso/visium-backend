package com.visium.backend.dto.usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class CambiarPasswordUsuarioRequest {
  @NotBlank @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
  private String nuevaPassword;

  @NotBlank(message = "La contrasena de confirmacion es obligatoria")
  private String passwordConfirmacion;
}
