package com.visium.backend.service;

import com.visium.backend.dto.consulta.CerrarCitaConsultaRequest;
import com.visium.backend.dto.consulta.ConsultaResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Consulta;
import com.visium.backend.entity.Paciente;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.mapper.ConsultaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.PacienteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaService {

  private final ConsultaRepository consultaRepository;
  private final CitaRepository citaRepository;
  private final PacienteRepository pacienteRepository;
  private final ConsultaMapper consultaMapper;
  private final AccesoService accesoService;

  @Transactional
  public ConsultaResponse cerrarCita(CerrarCitaConsultaRequest request) {
    Cita cita = buscarCitaOFallar(request.getCitaId());
    validarCitaEnEmpresaYSucursalAutorizada(cita);

    if (cita.getEstado() != EstadoCita.CONFIRMADA) {
      throw new BadRequestException("Solo se puede cerrar una cita CONFIRMADA");
    }

    consultaRepository
        .findByCitaId(cita.getId())
        .ifPresent(
            consulta -> {
              throw new BadRequestException("La cita ya tiene una consulta registrada");
            });

    Consulta consulta = consultaMapper.toEntity(request, cita);
    consulta.setFechaFin(Instant.now());
    cita.setEstado(EstadoCita.ATENDIDA);

    consulta = consultaRepository.save(consulta);
    citaRepository.save(cita);
    return consultaMapper.toResponse(consulta);
  }

  @Transactional(readOnly = true)
  public ConsultaResponse obtenerPorId(UUID id) {
    Consulta consulta = buscarConsultaOFallar(id);
    validarCitaEnEmpresaYSucursalAutorizada(consulta.getCita());
    return consultaMapper.toResponse(consulta);
  }

  @Transactional(readOnly = true)
  public List<ConsultaResponse> listarPorPaciente(UUID pacienteId) {
    Paciente paciente =
        pacienteRepository
            .findById(pacienteId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado: " + pacienteId));

    accesoService.exigirAccesoEmpresa(paciente.getEmpresa().getId());

    return consultaRepository.findByCitaPacienteIdOrderByCreatedAtDesc(pacienteId).stream()
        .peek(consulta -> validarCitaEnEmpresaYSucursalAutorizada(consulta.getCita()))
        .map(consultaMapper::toResponse)
        .toList();
  }

  private void validarCitaEnEmpresaYSucursalAutorizada(Cita cita) {
    UUID empresaId = cita.getEmpresaId();
    UUID sucursalId = cita.getSucursal().getId();

    if (!cita.getPaciente().getEmpresa().getId().equals(empresaId)) {
      throw new BadRequestException("El paciente de la cita no pertenece a la empresa de la cita");
    }
    if (!cita.getSucursal().getEmpresa().getId().equals(empresaId)) {
      throw new BadRequestException("La sucursal de la cita no pertenece a la empresa de la cita");
    }

    accesoService.exigirAccesoSucursal(empresaId, sucursalId);
  }

  private Cita buscarCitaOFallar(UUID id) {
    return citaRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + id));
  }

  private Consulta buscarConsultaOFallar(UUID id) {
    return consultaRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada: " + id));
  }
}
