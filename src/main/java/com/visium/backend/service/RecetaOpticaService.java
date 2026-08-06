package com.visium.backend.service;

import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.entity.RecetaOpticaDetalle;
import com.visium.backend.entity.Consulta;
import com.visium.backend.dto.receta.RecetaOpticaRequest;
import com.visium.backend.dto.receta.RecetaHistorialResponse;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.PacienteRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.RecetaOpticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class RecetaOpticaService {

    private final RecetaOpticaRepository recetaOpticaRepository;
    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final SucursalRepository sucursalRepository;
    private final AccesoService accesoService;
    private final EmailService emailService;

    @Transactional
    public RecetaOptica guardarReceta(RecetaOpticaRequest request) {
        var paciente = pacienteRepository.findById(request.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        var empresaId = accesoService.resolverEmpresaObjetivo(request.getEmpresaId());
        var sucursalId = accesoService.resolverSucursalObjetivo(empresaId, request.getSucursalId());
        if (!empresaId.equals(paciente.getEmpresa().getId())) throw new BadRequestException("El paciente no pertenece a la empresa activa");
        var sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        Consulta consulta = request.getConsultaId() == null ? null : consultaRepository.findById(request.getConsultaId())
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada"));
        if (consulta != null && recetaOpticaRepository.findByConsultaId(consulta.getId()).isPresent()) {
            throw new BadRequestException("La consulta ya tiene una receta emitida");
        }
        RecetaOptica receta = new RecetaOptica();
        receta.setConsulta(consulta);
        receta.setPaciente(paciente);
        receta.setSucursal(sucursal);
        receta.setFechaEmision(request.getFechaEmision());
        receta.setAdicion(request.getAdicion());
        receta.setDistanciaPupilar(request.getDistanciaPupilar());
        receta.setIndicaciones(request.getIndicaciones());
        receta.setObservaciones(request.getObservaciones());
        HashSet<com.visium.backend.enums.Ojo> ojos = new HashSet<>();
        for (var detalleRequest : request.getDetalles() == null ? java.util.List.<com.visium.backend.dto.receta.RecetaOpticaDetalleRequest>of() : request.getDetalles()) {
            if (!ojos.add(detalleRequest.getOjo())) throw new BadRequestException("No se puede repetir un ojo en la receta");
            RecetaOpticaDetalle detalle = new RecetaOpticaDetalle();
            detalle.setOjo(detalleRequest.getOjo()); detalle.setEsfera(detalleRequest.getEsfera());
            detalle.setCilindro(detalleRequest.getCilindro()); detalle.setEje(detalleRequest.getEje());
            receta.agregarDetalle(detalle);
        }
        RecetaOptica recetaGuardada = recetaOpticaRepository.save(receta);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { emailService.enviarRecetaAutomatica(recetaGuardada.getId()); }
        });
        return recetaGuardada;
    }

    @Transactional(readOnly = true)
    public void reenviarReceta(java.util.UUID recetaId) {
        RecetaOptica receta = recetaOpticaRepository.findById(recetaId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));
        accesoService.exigirAccesoSucursal(receta.getPaciente().getEmpresa().getId(), receta.getSucursal().getId());
        emailService.enviarRecetaAutomatica(recetaId);
    }

    @Transactional(readOnly = true)
    public java.util.List<RecetaHistorialResponse> listarHistorial(java.util.UUID pacienteId) {
        return recetaOpticaRepository.findHistorialByPacienteId(pacienteId).stream().map(receta -> {
            accesoService.exigirAccesoSucursal(receta.getPaciente().getEmpresa().getId(), receta.getSucursal().getId());
            var detalles = receta.getDetalles().stream().map(detalle -> {
                var dto = new com.visium.backend.dto.receta.RecetaOpticaDetalleRequest();
                dto.setOjo(detalle.getOjo()); dto.setEsfera(detalle.getEsfera()); dto.setCilindro(detalle.getCilindro()); dto.setEje(detalle.getEje());
                return dto;
            }).toList();
            return new RecetaHistorialResponse(receta.getId(), receta.getFechaEmision(), receta.getAdicion(), receta.getDistanciaPupilar(), receta.getIndicaciones(), receta.getObservaciones(), detalles);
        }).toList();
    }
}
