package com.visium.backend.dto.recepcionista;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Datos para crear o editar un recepcionista.
 * El rol siempre es RECEPCIONISTA (no se recibe del cliente).
 */
@Getter
@Setter
public class RecepcionistaRequest {

	@NotNull(message = "La empresa es obligatoria")
	private UUID empresaId;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	@Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$", message = "El nombre solo puede contener letras y espacios")
	private String nombre;

	@NotBlank(message = "El apellido es obligatorio")
	@Size(max = 100)
	@Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$", message = "El apellido solo puede contener letras y espacios")
	private String apellido;

	@NotBlank(message = "El email es obligatorio")
	@Email
	@Size(max = 254)
	private String email;

	@Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
	private String password;

	@Pattern(regexp = "^$|^[0-9]{7,8}-[0-9Kk]$", message = "El RUT debe tener formato 12345678-9")
	private String run;

	@Pattern(regexp = "^$|^\\+?[0-9 ]{8,15}$", message = "El teléfono solo puede contener números, espacios y +")
	private String telefono;

	/** Sucursales asignadas; si viene en la edicion, reemplaza las anteriores. */
	@NotEmpty(message = "Debe seleccionar al menos una sucursal")
	private List<UUID> sucursalIds;
}
