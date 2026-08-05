package com.visium.backend.repository;

import com.visium.backend.entity.UsuarioSucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso a la tabla usuarios_sucursales.
 */
public interface UsuarioSucursalRepository extends JpaRepository<UsuarioSucursal, UsuarioSucursal.UsuarioSucursalId> {

	List<UsuarioSucursal> findByUsuarioEmpresaId(UUID usuarioEmpresaId);

	void deleteByUsuarioEmpresaId(UUID usuarioEmpresaId);
}
