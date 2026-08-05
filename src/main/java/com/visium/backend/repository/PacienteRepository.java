package com.visium.backend.repository;

import com.visium.backend.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Acceso a la tabla pacientes.
 */
public interface PacienteRepository extends JpaRepository<Paciente, UUID> {

	List<Paciente> findByEmpresaId(UUID empresaId);

	/**
	 * Busqueda paginada por texto en nombre, apellido, documento, email o telefono,
	 * dentro de una empresa. Si texto es null o vacio, no filtra por texto.
	 */
	@Query("""
			SELECT p FROM Paciente p
			WHERE p.empresa.id = :empresaId
			  AND (:texto IS NULL OR :texto = ''
			       OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR LOWER(COALESCE(p.numeroDocumento, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR LOWER(COALESCE(p.telefono, '')) LIKE LOWER(CONCAT('%', :texto, '%')))
			""")
	Page<Paciente> buscarPorEmpresa(
			@Param("empresaId") UUID empresaId,
			@Param("texto") String texto,
			Pageable pageable
	);

	long countByEmpresaId(UUID empresaId);
}
