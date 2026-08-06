package com.visium.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Informacion especifica de un profesional (optometrista / oftalmologo).
 * Todo profesional es un usuario, pero no todo usuario es profesional.
 * Tabla: profesionales
 */
@Entity
@Table(name = "profesionales")
@Getter
@Setter
@NoArgsConstructor
public class Profesional {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Relación histórica; los nuevos profesionales no crean cuenta de acceso. */
	@OneToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "usuario_id", nullable = true, unique = true)
	private Usuario usuario;

	@Column(name = "empresa_id")
	private UUID empresaId;

	@Column(name = "sucursal_id")
	private UUID sucursalId;

	@Column(length = 100)
	private String nombre;

	@Column(length = 100)
	private String apellido;

	@Column(length = 254)
	private String email;

	@Column(length = 12)
	private String run;

	@Column(length = 30)
	private String telefono;

	@Column(name = "numero_registro", nullable = false, unique = true, length = 50)
	private String numeroRegistro;

	@Column(nullable = false, length = 120)
	private String especialidad;

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
