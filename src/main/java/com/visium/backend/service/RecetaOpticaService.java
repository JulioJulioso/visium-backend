package com.visium.backend.service;

import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.repository.RecetaOpticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecetaOpticaService {

    private final RecetaOpticaRepository recetaOpticaRepository;
    private final EmailService emailService;

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