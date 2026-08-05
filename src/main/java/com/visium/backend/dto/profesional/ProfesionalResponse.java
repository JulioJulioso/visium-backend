package com.visium.backend.dto.profesional;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProfesionalResponse {

	private UUID id;
	private UUID usuarioId;
	private UUID empresaId;
	private String nombre;
	private String apellido;
	private String email;
	private String especialidad;
	private Boolean activo;
	private List<UUID> sucursalIds;
}
