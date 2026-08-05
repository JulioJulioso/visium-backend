package com.visium.backend.dto.profesional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Datos para registrar un profesional (crea usuario + rol + perfil + sucursales).
 */
@Getter
@Setter
public class ProfesionalRequest {

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

	@NotBlank(message = "La contrasena es obligatoria")
	@Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
	private String password;

	@Size(max = 12)
	private String run;

	@Size(max = 30)
	private String telefono;

	@NotBlank(message = "La especialidad es obligatoria")
	@Size(max = 120)
	private String especialidad;

	@NotEmpty(message = "Debe asignar al menos una sucursal")
	private List<UUID> sucursalIds;
}
