package com.visium.backend.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Datos para crear o editar un usuario.
 * El rol se asigna desde el portal del administrador (opcional aqui); al editar,
 * si viene, reemplaza los roles anteriores. La contrasena es obligatoria en el alta.
 */
@Getter
@Setter
public class UsuarioRequest {

	@jakarta.validation.constraints.NotNull(message = "La empresa es obligatoria")
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

	/** Codigo del rol a asignar (SUPER_ADMIN, JEFE, ADMINISTRADOR_SUCURSALES, JEFE_SUCURSAL, RECEPCIONISTA, PROFESIONAL). */
	@Size(max = 50)
	private String rol;

	/** Sucursales asignadas; si viene en la edicion, reemplaza las anteriores. */
	private List<UUID> sucursalIds;
}
