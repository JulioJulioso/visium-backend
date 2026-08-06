package com.visium.backend.dto.paciente;

import com.visium.backend.enums.Sexo;
import com.visium.backend.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PacienteRequest {

	@NotNull(message = "La empresa es obligatoria")
	private UUID empresaId;

	private UUID sucursalId;

	private TipoDocumento tipoDocumento;

	@Size(max = 50)
	private String numeroDocumento;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	private String nombre;

	@NotBlank(message = "El apellido es obligatorio")
	@Size(max = 100)
	private String apellido;

	private LocalDate fechaNacimiento;

	private Sexo sexo;

	@Size(max = 30)
	private String telefono;

	@Size(max = 254)
	private String email;

	private String direccion;

	private Boolean activo;
}
