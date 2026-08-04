package com.visium.backend.controller;

import com.visium.backend.dto.recepcionista.RecepcionistaRequest;
import com.visium.backend.dto.recepcionista.RecepcionistaResponse;
import com.visium.backend.service.RecepcionistaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de recepcionistas.
 * Registro: SUPER_ADMIN o JEFE. Listado filtrado por AccesoService.
 */
@RestController
@RequestMapping("/recepcionistas")
@RequiredArgsConstructor
public class RecepcionistaController {

    private final RecepcionistaService recepcionistaService;

    @GetMapping
    public ResponseEntity<List<RecepcionistaResponse>> listar() {
        return ResponseEntity.ok(recepcionistaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecepcionistaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(recepcionistaService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE')")
    public ResponseEntity<RecepcionistaResponse> registrar(
            @Valid @RequestBody RecepcionistaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recepcionistaService.registrar(request));
    }
}