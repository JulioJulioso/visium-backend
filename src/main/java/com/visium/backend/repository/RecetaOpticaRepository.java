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

	// Inicializa todas las relaciones LAZY para poder serializar y generar PDF fuera de transacción
	@Query("""
			SELECT r FROM RecetaOptica r
			JOIN FETCH r.consulta c
			JOIN FETCH c.cita ci
			JOIN FETCH ci.paciente p
			JOIN FETCH ci.sucursal s
			JOIN FETCH s.empresa
			LEFT JOIN FETCH ci.profesional pr
			LEFT JOIN FETCH r.detalles d
			WHERE p.id = :pacienteId
			ORDER BY r.createdAt DESC
			""")
	List<RecetaOptica> findHistorialByPacienteId(@Param("pacienteId") UUID pacienteId);

	@Query("""
			SELECT r FROM RecetaOptica r
			JOIN FETCH r.consulta c
			JOIN FETCH c.cita ci
			JOIN FETCH ci.paciente p
			JOIN FETCH ci.sucursal s
			JOIN FETCH s.empresa
			LEFT JOIN FETCH ci.profesional pr
			LEFT JOIN FETCH r.detalles d
			WHERE r.id = :id
			""")
	Optional<RecetaOptica> findByIdConRelaciones(@Param("id") UUID id);
}
