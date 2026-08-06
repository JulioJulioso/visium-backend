package com.visium.backend.entity;

import com.visium.backend.enums.Sexo;
import com.visium.backend.enums.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa a una persona atendida en una empresa.
 * Los pacientes no inician sesion (no son usuarios).
 * Tabla: pacientes
 */
@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
public class Paciente {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(name = "sucursal_id")
	private UUID sucursalId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", nullable = false, length = 20)
	private TipoDocumento tipoDocumento = TipoDocumento.RUN;

	@Column(name = "numero_documento", length = 50)
	private String numeroDocumento;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, length = 100)
	private String apellido;

	@Column(name = "fecha_nacimiento")
	private LocalDate fechaNacimiento;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Sexo sexo;

	@Column(length = 30)
	private String telefono;

	@Column(length = 254)
	private String email;

	@Column(columnDefinition = "TEXT")
	private String direccion;

	@Column(nullable = false)
	private Boolean activo = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void alCrear() {
		Instant ahora = Instant.now();
		createdAt = ahora;
		updatedAt = ahora;
	}

	@PreUpdate
	void alActualizar() {
		updatedAt = Instant.now();
	}
}
