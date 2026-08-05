package com.visium.backend.repository;

import com.visium.backend.entity.Cita;
import com.visium.backend.enums.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Acceso a la tabla citas.
 */
public interface CitaRepository extends JpaRepository<Cita, UUID> {

	List<Cita> findByEmpresaId(UUID empresaId);

	List<Cita> findByEmpresaIdAndEstado(UUID empresaId, EstadoCita estado);

	List<Cita> findByProfesionalIdAndFechaHoraInicioBetween(
			UUID profesionalId,
			Instant desde,
			Instant hasta
	);

	List<Cita> findByProfesionalIdAndFechaHoraInicioBetweenAndEstado(
			UUID profesionalId,
			Instant desde,
			Instant hasta,
			EstadoCita estado
	);

	List<Cita> findByProfesionalIdAndEstadoOrderByFechaHoraInicioAsc(
			UUID profesionalId,
			EstadoCita estado
	);
}
