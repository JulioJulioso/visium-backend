package com.visium.backend.repository;

import com.visium.backend.entity.Paciente;
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

	List<Paciente> findByEmpresaIdAndSucursalId(UUID empresaId, UUID sucursalId);

	@Query("""
			SELECT paciente FROM Paciente paciente
			WHERE paciente.empresa.id = :empresaId
			  AND (LOWER(paciente.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
			       OR LOWER(paciente.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
			       OR LOWER(paciente.numeroDocumento) LIKE LOWER(CONCAT('%', :termino, '%'))
			       OR LOWER(REPLACE(REPLACE(REPLACE(paciente.numeroDocumento, '.', ''), '-', ''), ' ', ''))
			          LIKE LOWER(CONCAT('%', :documentoNormalizado, '%')))
			ORDER BY paciente.nombre, paciente.apellido
			""")
	List<Paciente> buscarPorEmpresa(
			@Param("empresaId") UUID empresaId,
			@Param("termino") String termino,
			@Param("documentoNormalizado") String documentoNormalizado);
}
