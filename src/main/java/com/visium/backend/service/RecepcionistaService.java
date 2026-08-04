package com.visium.backend.service;

import com.visium.backend.dto.recepcionista.RecepcionistaRequest;
import com.visium.backend.dto.recepcionista.RecepcionistaResponse;
import com.visium.backend.entity.Recepcionista;
import com.visium.backend.repository.RecepcionistaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecepcionistaService {

    private final RecepcionistaRepository recepcionistaRepository;

    public List<RecepcionistaResponse> listar() {
        return recepcionistaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public RecepcionistaResponse obtenerPorId(UUID id) {
        Recepcionista recepcionista = recepcionistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recepcionista no encontrado"));
        return mapToResponse(recepcionista);
    }

    public RecepcionistaResponse registrar(RecepcionistaRequest request) {
        Recepcionista recepcionista = Recepcionista.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .activo(true)
                .build();

        Recepcionista guardado = recepcionistaRepository.save(recepcionista);
        return mapToResponse(guardado);
    }

    private RecepcionistaResponse mapToResponse(Recepcionista recepcionista) {
        return RecepcionistaResponse.builder()
                .id(recepcionista.getId())
                .nombre(recepcionista.getNombre())
                .apellido(recepcionista.getApellido())
                .email(recepcionista.getEmail())
                .telefono(recepcionista.getTelefono())
                .activo(recepcionista.getActivo())
                .build();
    }
}