package com.visium.backend.service;

import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.security.EmpresaContext;
import com.visium.backend.security.UsuarioDetails;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Decide qué empresas/sucursales puede ver o tocar el usuario autenticado.
 * Una sola BD; el aislamiento es por ids.
 */
@Service
public class AccesoService {

	public UsuarioDetails usuarioActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof UsuarioDetails detalles)) {
			throw new ForbiddenException("No hay un usuario autenticado");
		}
		return detalles;
	}

	public boolean esSuperAdmin() {
		return usuarioActual().esSuperAdmin();
	}

	/** JEFE = dueño de óptica. */
	public boolean esJefeDeEmpresa() {
		return usuarioActual().getRoles().contains("JEFE");
	}

	/** Catálogo de empresas: plataforma o jefe que administra más de una empresa. */
	public boolean puedeGestionarCatalogoEmpresas() {
		return esSuperAdmin() || (esJefeDeEmpresa() && usuarioActual().getEmpresaIds().size() > 1);
	}

	/**
	 * Roles que solo ven sucursales asignadas (no toda la óptica).
	 */
	public boolean tieneAlcanceSoloSucursalesAsignadas() {
		if (esSuperAdmin() || esJefeDeEmpresa()) {
			return false;
		}
		return !usuarioActual().getSucursalIds().isEmpty();
	}

	public boolean puedeAccederEmpresa(UUID empresaId) {
		if (empresaId == null) {
			return false;
		}
		UsuarioDetails u = usuarioActual();
		return u.esSuperAdmin() || u.perteneceAEmpresa(empresaId);
	}

	public void exigirAccesoEmpresa(UUID empresaId) {
		if (!puedeAccederEmpresa(empresaId)) {
			throw new ForbiddenException("No tienes acceso a esa empresa");
		}
	}

	/**
	 * Sucursal: SUPER_ADMIN y JEFE ven todas de su empresa;
	 * JEFE_SUCURSAL / RECEPCIONISTA / PROFESIONAL solo las de usuarios_sucursales.
	 */
	public void exigirAccesoSucursal(UUID empresaId, UUID sucursalId) {
		exigirAccesoEmpresa(empresaId);
		if (esSuperAdmin() || esJefeDeEmpresa()) {
			return;
		}
		if (!usuarioActual().getSucursalIds().contains(sucursalId)) {
			throw new ForbiddenException("No tienes acceso a esa sucursal");
		}
	}

	/**
	 * Resuelve la empresa a usar: parametro del request, o EmpresaContext, o unica empresa.
	 * SUPER_ADMIN puede operar sin pertenencia si envia empresaId / header.
	 */
	public UUID resolverEmpresaObjetivo(UUID empresaIdSolicitada) {
		if (empresaIdSolicitada != null) {
			exigirAccesoEmpresa(empresaIdSolicitada);
			return empresaIdSolicitada;
		}

		UUID delContexto = EmpresaContext.getEmpresaId();
		if (delContexto != null) {
			exigirAccesoEmpresa(delContexto);
			return delContexto;
		}

		List<UUID> propias = usuarioActual().getEmpresaIds();
		if (propias.size() == 1) {
			return propias.getFirst();
		}
		if (esSuperAdmin()) {
			throw new BadRequestException(
					"Indica la empresa con X-Empresa-Id o el parametro empresaId");
		}
		if (propias.isEmpty()) {
			throw new ForbiddenException("No tienes empresas asignadas");
		}
		throw new BadRequestException(
				"Tienes varias empresas: envia el header X-Empresa-Id");
	}

	/** Empresas visibles en un listado. */
	public List<UUID> empresaIdsVisibles() {
		if (esSuperAdmin()) {
			return List.of(); // vacio = sin filtro (todas)
		}
		return List.copyOf(usuarioActual().getEmpresaIds());
	}

	public boolean veTodasLasEmpresas() {
		return esSuperAdmin();
	}

	/** Sucursales visibles dentro de una empresa ya autorizada. Vacio = todas. */
	public List<UUID> sucursalIdsVisiblesEnEmpresa() {
		if (esSuperAdmin() || esJefeDeEmpresa()) {
			return List.of();
		}
		return List.copyOf(usuarioActual().getSucursalIds());
	}

	public void exigirSuperAdmin() {
		if (!esSuperAdmin()) {
			throw new ForbiddenException("Solo SUPER_ADMIN puede realizar esta accion");
		}
	}
}
