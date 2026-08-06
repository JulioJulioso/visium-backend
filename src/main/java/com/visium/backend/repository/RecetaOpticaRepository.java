package com.visium.backend.repository;

import com.visium.backend.entity.RecetaOptica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a la tabla recetas_opticas.
 */
public interface RecetaOpticaRepository extends JpaRepository<RecetaOptica, UUID> {

	Optional<RecetaOptica> findByConsultaId(UUID consultaId);

	@Query("SELECT r FROM RecetaOptica r LEFT JOIN r.consulta consulta LEFT JOIN consulta.cita cita WHERE r.paciente.id = :pacienteId OR cita.paciente.id = :pacienteId ORDER BY r.createdAt DESC")
    List<RecetaOptica> findHistorialByPacienteId(@Param("pacienteId")UUID pacienteId);
}
