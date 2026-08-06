package com.visium.backend.service;

import com.visium.backend.dto.paciente.PacienteRequest;
import com.visium.backend.dto.paciente.PacienteResponse;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.FichaClinica;
import com.visium.backend.entity.Paciente;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.mapper.PacienteMapper;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.FichaClinicaRepository;
import com.visium.backend.repository.PacienteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PacienteService {

	private final PacienteRepository pacienteRepository;
	private final EmpresaRepository empresaRepository;
	private final FichaClinicaRepository fichaClinicaRepository;
	private final PacienteMapper pacienteMapper;
	private final AccesoService accesoService;

	@Transactional(readOnly = true)
	public List<PacienteResponse> listarPorEmpresa(UUID empresaId) {
		return listarPorEmpresa(empresaId, null);
	}

	@Transactional(readOnly = true)
	public List<PacienteResponse> listarPorEmpresa(UUID empresaId, UUID sucursalId) {
		UUID empresa = accesoService.resolverEmpresaObjetivo(empresaId);
		if (sucursalId != null) {
			accesoService.exigirAccesoSucursal(empresa, sucursalId);
		}
		return (sucursalId == null
				? pacienteRepository.findByEmpresaId(empresa)
				: pacienteRepository.findByEmpresaIdAndSucursalId(empresa, sucursalId)).stream()
				.map(pacienteMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PacienteResponse> buscarPorEmpresa(UUID empresaId, String termino, UUID sucursalId) {
		UUID empresa = accesoService.resolverEmpresaObjetivo(empresaId);
		String busqueda = termino == null ? "" : termino.trim();
		String documentoNormalizado = busqueda.replaceAll("[.\\-\\s]", "");
		if (busqueda.isEmpty()) {
			return listarPorEmpresa(empresa, sucursalId);
		}
		if (sucursalId != null) {
			accesoService.exigirAccesoSucursal(empresa, sucursalId);
		}
		if (sucursalId != null) {
			return pacienteRepository.buscarPorEmpresa(empresa, busqueda, documentoNormalizado).stream()
					.filter(paciente -> sucursalId.equals(paciente.getSucursalId()))
					.map(pacienteMapper::toResponse)
					.toList();
		}
		return pacienteRepository.buscarPorEmpresa(empresa, busqueda, documentoNormalizado).stream()
				.map(pacienteMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PacienteResponse obtenerPorId(UUID id) {
		Paciente paciente = buscarOFallar(id);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		return pacienteMapper.toResponse(paciente);
	}

	@Transactional
	public PacienteResponse crear(PacienteRequest request) {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		request.setSucursalId(accesoService.resolverSucursalObjetivo(empresaId, request.getSucursalId()));
		Empresa empresa = verificarEmpresa(empresaId);
		Paciente paciente = pacienteMapper.toEntity(request, empresa);
		if (paciente.getActivo() == null) {
			paciente.setActivo(true);
		}
		paciente = pacienteRepository.save(paciente);

		FichaClinica ficha = new FichaClinica();
		ficha.setPaciente(paciente);
		fichaClinicaRepository.save(ficha);

		return pacienteMapper.toResponse(paciente);
	}

	@Transactional
	public PacienteResponse actualizar(UUID id, PacienteRequest request) {
		Paciente paciente = buscarOFallar(id);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		UUID empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
		request.setSucursalId(accesoService.resolverSucursalObjetivo(empresaId, request.getSucursalId()));
		Empresa empresa = verificarEmpresa(empresaId);
		paciente.setEmpresa(empresa);
		pacienteMapper.aplicar(paciente, request);
		return pacienteMapper.toResponse(pacienteRepository.save(paciente));
	}

	@Transactional
	public void desactivar(UUID id) {
		Paciente paciente = buscarOFallar(id);
		accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());
		paciente.setActivo(false);
		pacienteRepository.save(paciente);
	}

	private Empresa verificarEmpresa(UUID empresaId) {
		return empresaRepository.findById(empresaId)
				.orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));
	}

	private Paciente buscarOFallar(UUID id) {
		return pacienteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + id));
	}
}
