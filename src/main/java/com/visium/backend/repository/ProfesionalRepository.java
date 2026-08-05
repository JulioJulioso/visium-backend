package com.visium.backend.repository;

import com.visium.backend.entity.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a la tabla profesionales.
 */
public interface ProfesionalRepository extends JpaRepository<Profesional, UUID> {

	Optional<Profesional> findByUsuarioId(UUID usuarioId);

}
