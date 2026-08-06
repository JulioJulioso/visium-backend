package com.visium.backend.dto.rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolRequest {
  @NotBlank(message = "El código es obligatorio")
  @Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "El código debe usar mayúsculas, números o guion bajo")
  @Size(max = 50)
  private String codigo;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 100)
  private String nombre;
}
