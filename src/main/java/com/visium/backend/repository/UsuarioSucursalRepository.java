package com.visium.backend.repository;

import com.visium.backend.entity.UsuarioSucursal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla usuarios_sucursales.
 */
public interface UsuarioSucursalRepository extends JpaRepository<UsuarioSucursal, UsuarioSucursal.UsuarioSucursalId> {

	List<UsuarioSucursal> findByUsuarioEmpresaId(UUID usuarioEmpresaId);

}
