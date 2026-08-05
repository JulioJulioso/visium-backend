package com.visium.backend.repository;

import com.visium.backend.entity.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a la tabla usuarios_empresas.
 */
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UUID> {

	Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

	List<UsuarioEmpresa> findByUsuarioId(UUID usuarioId);

	List<UsuarioEmpresa> findByEmpresaId(UUID empresaId);
}
