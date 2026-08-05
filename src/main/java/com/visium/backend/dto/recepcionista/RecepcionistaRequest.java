package com.visium.backend.dto.recepcionista;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para crear o editar un recepcionista. El rol siempre es RECEPCIONISTA (no se recibe del
 * cliente).
 */
@Getter
@Setter
public class RecepcionistaRequest {

  @NotNull(message = "La empresa es obligatoria")
  private UUID empresaId;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 100)
  private String nombre;

  @NotBlank(message = "El apellido es obligatorio")
  @Size(max = 100)
  private String apellido;

  @NotBlank(message = "El email es obligatorio")
  @Email
  @Size(max = 254)
  private String email;

  @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
  private String password;

  @Size(max = 12)
  private String run;

  @Size(max = 30)
  private String telefono;

  /** Sucursales asignadas; si viene en la edicion, reemplaza las anteriores. */
  private List<UUID> sucursalIds;
}

