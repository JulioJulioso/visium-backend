package com.visium.backend.repository;

import com.visium.backend.entity.Profesional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la tabla profesionales.
 */
public interface ProfesionalRepository extends JpaRepository<Profesional, UUID> {

	Optional<Profesional> findByUsuarioId(UUID usuarioId);

	Optional<Profesional> findByNumeroRegistro(String numeroRegistro);

	/**
	 * Obtiene los profesionales asignados a una sucursal especifica
	 * navegando a traves de las entidades Usuario, UsuarioEmpresa y UsuarioSucursal.
	 */
	@Query("""
        SELECT DISTINCT p FROM Profesional p
        JOIN p.usuario u
        JOIN UsuarioEmpresa ue ON ue.usuario.id = u.id
        JOIN UsuarioSucursal us ON us.usuarioEmpresaId = ue.id
        WHERE us.sucursalId = :sucursalId
          AND p.activo = true
          AND u.activo = true
          AND ue.activo = true
    """)
	List<Profesional> findBySucursalId(@Param("sucursalId") UUID sucursalId);
}