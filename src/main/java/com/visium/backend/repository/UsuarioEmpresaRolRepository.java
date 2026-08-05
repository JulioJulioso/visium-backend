package com.visium.backend.repository;

import com.visium.backend.entity.UsuarioEmpresaRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso a la tabla usuarios_empresas_roles.
 */
public interface UsuarioEmpresaRolRepository extends JpaRepository<UsuarioEmpresaRol, UsuarioEmpresaRol.UsuarioEmpresaRolId> {

	List<UsuarioEmpresaRol> findByUsuarioEmpresaId(UUID usuarioEmpresaId);

	void deleteByUsuarioEmpresaId(UUID usuarioEmpresaId);
}
