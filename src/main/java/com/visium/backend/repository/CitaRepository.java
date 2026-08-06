package com.visium.backend.repository;

import com.visium.backend.entity.Cita;
import com.visium.backend.enums.EstadoCita;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/**
	 * Cuenta citas confirmadas dentro de un rango de fechas, para las sucursales indicadas.
	 * Si sucursalIds es null o vacio, cuenta en toda la empresa.
	 */
	@Query("""
			SELECT COUNT(c) FROM Cita c
			WHERE c.empresaId = :empresaId
			  AND c.estado = :estado
			  AND c.fechaHoraInicio >= :desde
			  AND c.fechaHoraInicio < :hasta
			  AND (:sucursalIds IS NULL OR c.sucursal.id IN :sucursalIds)
			""")
	long contarEnRango(
			@Param("empresaId") UUID empresaId,
			@Param("sucursalIds") List<UUID> sucursalIds,
			@Param("estado") EstadoCita estado,
			@Param("desde") Instant desde,
			@Param("hasta") Instant hasta
	);

	/**
	 * Proximas citas (a partir de ahora) para las sucursales indicadas.
	 * Si sucursalIds es null o vacio, toma todas las de la empresa.
	 */
	@Query("""
			SELECT c FROM Cita c
			WHERE c.empresaId = :empresaId
			  AND c.estado = :estado
			  AND c.fechaHoraInicio >= :desde
			  AND (:sucursalIds IS NULL OR c.sucursal.id IN :sucursalIds)
			ORDER BY c.fechaHoraInicio ASC
			""")
	List<Cita> proximas(
			@Param("empresaId") UUID empresaId,
			@Param("sucursalIds") List<UUID> sucursalIds,
			@Param("estado") EstadoCita estado,
			@Param("desde") Instant desde,
			Pageable pageable
	);

	/**
	 * Listado de citas de la empresa en un rango de fechas, para las sucursales indicadas.
	 * Si sucursalIds es null o vacio, toma todas las de la empresa.
	 * El estado es opcional: si es null, devuelve citas de cualquier estado.
	 */
	@Query("""
			SELECT c FROM Cita c
			WHERE c.empresaId = :empresaId
			  AND (:estado IS NULL OR c.estado = :estado)
			  AND c.fechaHoraInicio >= :desde
			  AND c.fechaHoraInicio < :hasta
			  AND (:sucursalIds IS NULL OR c.sucursal.id IN :sucursalIds)
			ORDER BY c.fechaHoraInicio ASC
			""")
	List<Cita> listarEnRango(
			@Param("empresaId") UUID empresaId,
			@Param("sucursalIds") List<UUID> sucursalIds,
			@Param("estado") EstadoCita estado,
			@Param("desde") Instant desde,
			@Param("hasta") Instant hasta
	);
}
