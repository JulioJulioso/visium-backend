package com.visium.backend.service;

import com.visium.backend.dto.sucursal.SucursalRequest;
import com.visium.backend.dto.sucursal.SucursalResponse;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.mapper.SucursalMapper;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.SucursalRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SucursalService {

	private final SucursalRepository sucursalRepository;
	private final EmpresaRepository empresaRepository;
	private final SucursalMapper sucursalMapper;
	private final AccesoService accesoService;

	@Transactional(readOnly = true)
	public List<SucursalResponse> listarPorEmpresa(UUID empresaId) {
		UUID empresa = accesoService.resolverEmpresaObjetivo(empresaId);
		List<UUID> sucursalesPermitidas = accesoService.sucursalIdsVisiblesEnEmpresa();

		return sucursalRepository.findByEmpresaId(empresa).stream()
				// Las instalaciones antiguas pueden conservar filas creadas antes de que
				// el contrato exigiera todos los datos. No son sucursales utilizables.
				.filter(this::esSucursalListable)
				.filter(s -> sucursalesPermitidas.isEmpty() || sucursalesPermitidas.contains(s.getId()))
				.map(sucursalMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public SucursalResponse obtenerPorId(UUID id) {
		Sucursal sucursal = buscarOFallar(id);
		accesoService.exigirAccesoSucursal(sucursal.getEmpresa().getId(), sucursal.getId());
		return sucursalMapper.toResponse(sucursal);
	}

	@Transactional
	public SucursalResponse crear(SucursalRequest request) {
		accesoService.exigirAccesoEmpresa(request.getEmpresaId());
		exigirGestionSucursales();

		Empresa empresa = verificarEmpresa(request.getEmpresaId());
		Sucursal sucursal = sucursalMapper.toEntity(request, empresa);
		if (sucursal.getActivo() == null) {
			sucursal.setActivo(true);
		}
		return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
	}

	@Transactional
	public SucursalResponse actualizar(UUID id, SucursalRequest request) {
		Sucursal sucursal = buscarOFallar(id);
		accesoService.exigirAccesoSucursal(sucursal.getEmpresa().getId(), sucursal.getId());
		accesoService.exigirAccesoEmpresa(request.getEmpresaId());
		exigirGestionSucursales();

		Empresa empresa = verificarEmpresa(request.getEmpresaId());
		sucursal.setEmpresa(empresa);
		sucursalMapper.aplicar(sucursal, request);
		return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
	}

	@Transactional
	public void desactivar(UUID id) {
		Sucursal sucursal = buscarOFallar(id);
		accesoService.exigirAccesoSucursal(sucursal.getEmpresa().getId(), sucursal.getId());
		exigirGestionSucursales();
		sucursal.setActivo(false);
		sucursalRepository.save(sucursal);
	}

	private void exigirGestionSucursales() {
		if (!accesoService.esSuperAdmin() && !accesoService.esJefeDeEmpresa()) {
			throw new ForbiddenException("Solo JEFE o SUPER_ADMIN pueden gestionar sucursales");
		}
	}

	private Empresa verificarEmpresa(UUID empresaId) {
		return empresaRepository.findById(empresaId)
				.orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));
	}

	private Sucursal buscarOFallar(UUID id) {
		return sucursalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + id));
	}

	private boolean esSucursalListable(Sucursal sucursal) {
		return sucursal != null
				&& sucursal.getId() != null
				&& sucursal.getEmpresa() != null
				&& sucursal.getEmpresa().getId() != null
				&& tieneTexto(sucursal.getNombre())
				&& tieneTexto(sucursal.getDireccion());
	}

	private boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}
}
