package com.visium.backend.dto.paciente;

import com.visium.backend.enums.Sexo;
import com.visium.backend.enums.TipoDocumento;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PacienteResponse {

	private UUID id;
	private UUID empresaId;
	private UUID sucursalId;
	private TipoDocumento tipoDocumento;
	private String numeroDocumento;
	private String nombre;
	private String apellido;
	private LocalDate fechaNacimiento;
	private Sexo sexo;
	private String telefono;
	private String email;
	private String direccion;
	private Boolean activo;
	private Instant createdAt;
	private Instant updatedAt;
}
