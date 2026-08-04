package com.visium.backend.dto.recepcionista;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecepcionistaResponse {

    private UUID id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private Boolean activo;
}