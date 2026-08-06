package com.visium.backend.service;

import com.visium.backend.dto.receta.RecetaDetalleRequest;
import com.visium.backend.dto.receta.RecetaRequest;
import com.visium.backend.dto.receta.RecetaResponse;
import com.visium.backend.entity.Consulta;
import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.entity.RecetaOpticaDetalle;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.RecetaOpticaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecetaOpticaService {

    private final RecetaOpticaRepository recetaOpticaRepository;
    private final ConsultaRepository consultaRepository;
    private final EmailService emailService;

    @Transactional
    public RecetaResponse crearReceta(RecetaRequest request) {
        Consulta consulta = consultaRepository
                .findById(request.getConsulta())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consulta no encontrada: " + request.getConsulta()));

        if (recetaOpticaRepository.findByConsultaId(consulta.getId()).isPresent()) {
            throw new BadRequestException("La consulta ya tiene una receta registrada");
        }

        RecetaOptica receta = new RecetaOptica();
        receta.setConsulta(consulta);
        receta.setAdicion(request.getAdicion());
        receta.setDistanciaPupilar(request.getDistanciaPupilar());
        receta.setIndicaciones(request.getIndicaciones());
        receta.setObservaciones(request.getObservaciones());

        if (request.getDetalles() != null) {
            for (RecetaDetalleRequest detalleRequest : request.getDetalles()) {
                if (detalleRequest.getOjo() == null) {
                    continue;
                }
                RecetaOpticaDetalle detalle = new RecetaOpticaDetalle();
                detalle.setOjo(detalleRequest.getOjo());
                detalle.setEsfera(detalleRequest.getEsfera());
                detalle.setCilindro(detalleRequest.getCilindro());
                detalle.setEje(detalleRequest.getEje());
                receta.agregarDetalle(detalle);
            }
        }

        RecetaOptica guardada = guardarReceta(receta);
        return toResponse(guardada);
    }

    public List<RecetaResponse> historialPorPaciente(UUID pacienteId) {
        return recetaOpticaRepository.findHistorialByPacienteId(pacienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    private RecetaResponse toResponse(RecetaOptica receta) {
        List<RecetaResponse.RecetaDetalleResponse> detalles = receta.getDetalles().stream()
                .map(detalle -> RecetaResponse.RecetaDetalleResponse.builder()
                        .ojo(detalle.getOjo())
                        .esfera(detalle.getEsfera())
                        .cilindro(detalle.getCilindro())
                        .eje(detalle.getEje())
                        .build())
                .toList();

        return RecetaResponse.builder()
                .id(receta.getId())
                .consultaId(receta.getConsulta().getId())
                .pacienteId(receta.getConsulta().getCita().getPaciente().getId())
                .fechaEmision(receta.getFechaEmision())
                .vigenciaHasta(receta.getVigenciaHasta())
                .adicion(receta.getAdicion())
                .distanciaPupilar(receta.getDistanciaPupilar())
                .indicaciones(receta.getIndicaciones())
                .observaciones(receta.getObservaciones())
                .detalles(detalles)
                .build();
    }

    @Transactional
    public RecetaOptica guardarReceta(RecetaOptica receta) {
        // 1. Guardamos la receta en PostgreSQL (aquí se le asigna su UUID)
        RecetaOptica recetaGuardada = recetaOpticaRepository.save(receta);

        // 2. ¡EL DISPARADOR! Llamamos al cartero pasándole el UUID recién creado
        // Como tiene @Async, esto no hará que la aplicación se quede pegada
        emailService.enviarRecetaAutomatica(recetaGuardada.getId());

        // 3. Devolvemos la receta guardada para que el Controller responda al Frontend
        return recetaGuardada;
    }
}
