package com.visium.backend.repository;

import com.visium.backend.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a la tabla consultas.
 */
public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {

	Optional<Consulta> findByCitaId(UUID citaId);

	List<Consulta> findByCitaPacienteIdOrderByCreatedAtDesc(UUID pacienteId);
}
