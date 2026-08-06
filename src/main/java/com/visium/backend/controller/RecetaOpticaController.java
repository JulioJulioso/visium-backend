package com.visium.backend.controller;

import com.visium.backend.dto.receta.RecetaRequest;
import com.visium.backend.dto.receta.RecetaResponse;
import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.RecetaOpticaRepository;
import com.visium.backend.service.RecetaOpticaService;
import com.visium.backend.service.RecetaPdfService;
import com.visium.backend.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recetas")
@RequiredArgsConstructor
public class RecetaOpticaController {

    private final RecetaOpticaRepository recetaOpticaRepository;
    private final RecetaPdfService pdfService;
    private final RecetaOpticaService recetaOpticaService;
    private final EmailService emailService;

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'PROFESIONAL')")
    @Operation(
            summary = "Historial de recetas de un paciente",
            description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o PROFESIONAL. "
                    + "Devuelve las recetas opticas del paciente.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<RecetaResponse>> historialPorPaciente(@PathVariable UUID pacienteId) {
        List<RecetaResponse> historial = recetaOpticaService.historialPorPaciente(pacienteId);
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'RECEPCIONISTA', 'PROFESIONAL')")
    @Operation(
            summary = "Descargar receta como PDF",
            description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE, RECEPCIONISTA o PROFESIONAL. "
                    + "Genera y descarga el PDF de una receta optica.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        RecetaOptica receta = recetaOpticaRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada: " + id));

        byte[] pdfBytes = pdfService.generarPdf(receta);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Receta_" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'PROFESIONAL')")
    @Operation(
            summary = "Crear receta optica",
            description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o PROFESIONAL. "
                    + "Registra una nueva receta optica asociada a una consulta.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<RecetaResponse> crearReceta(@Valid @RequestBody RecetaRequest request) {
        RecetaResponse nuevaReceta = recetaOpticaService.crearReceta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReceta);
    }

    // --------------------------------------------------------
    // ENDPOINT UNIFICADO: CREA EL PDF Y LO ENVÍA POR CORREO
    // --------------------------------------------------------
    @GetMapping("/test-unificado")
    @Operation(
            summary = "Probar PDF y Email juntos (Clon del Original)",
            description = "PUBLICO. Genera los datos falsos, crea el PDF y envía el correo con el mismo formato que el original.")
    public ResponseEntity<String> probarPdfYEmailUnificado() {

        // 1. Generamos los datos ficticios
        com.visium.backend.entity.Empresa empresa = new com.visium.backend.entity.Empresa();
        empresa.setRazonSocial("Óptica Visium Test");

        com.visium.backend.entity.Sucursal sucursal = new com.visium.backend.entity.Sucursal();
        sucursal.setNombre("Sucursal Centro");
        sucursal.setEmpresa(empresa);

        com.visium.backend.entity.Paciente paciente = new com.visium.backend.entity.Paciente();
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setNumeroDocumento("12.345.678-9");
        paciente.setFechaNacimiento(java.time.LocalDate.of(1990, 5, 15));

        com.visium.backend.entity.Cita cita = new com.visium.backend.entity.Cita();
        cita.setSucursal(sucursal);
        cita.setPaciente(paciente);

        com.visium.backend.entity.Consulta consulta = new com.visium.backend.entity.Consulta();
        consulta.setCita(cita);
        consulta.setDiagnostico("Miopía y Astigmatismo");

        com.visium.backend.entity.RecetaOptica receta = new com.visium.backend.entity.RecetaOptica();
        receta.setConsulta(consulta);
        receta.setFechaEmision(java.time.LocalDate.now());
        receta.setAdicion(new java.math.BigDecimal("2.50"));
        receta.setDistanciaPupilar(new java.math.BigDecimal("62.5"));
        receta.setObservaciones("Paciente requiere uso de lentes para lectura y pantallas.");

        com.visium.backend.entity.RecetaOpticaDetalle detalleOD = new com.visium.backend.entity.RecetaOpticaDetalle();
        detalleOD.setOjo(com.visium.backend.enums.Ojo.OD);
        detalleOD.setEsfera(new java.math.BigDecimal("-1.25"));
        detalleOD.setCilindro(new java.math.BigDecimal("-0.50"));
        detalleOD.setEje((short) 90);
        receta.agregarDetalle(detalleOD);

        com.visium.backend.entity.RecetaOpticaDetalle detalleOI = new com.visium.backend.entity.RecetaOpticaDetalle();
        detalleOI.setOjo(com.visium.backend.enums.Ojo.OI);
        detalleOI.setEsfera(new java.math.BigDecimal("-1.50"));
        detalleOI.setCilindro(new java.math.BigDecimal("-0.25"));
        detalleOI.setEje((short) 85);
        receta.agregarDetalle(detalleOI);

        // 2. Generamos los bytes del PDF usando tu servicio
        byte[] pdfBytes = pdfService.generarPdf(receta);

        // 3. Define aquí el correo donde quieres recibir la prueba
        String correoDestino = "estayjose3@gmail.com";

        // 4. Llamamos al servicio clon original, pasándole las variables dinámicas
        emailService.enviarCorreoPruebaConPdf(
                correoDestino,
                pdfBytes,
                paciente.getNombre(),
                sucursal.getNombre(),
                empresa.getRazonSocial()
        );

        // 5. Retornamos la respuesta a Bruno/Navegador
        return ResponseEntity.ok("Proceso unificado exitoso. El PDF se generó y se envió a " + correoDestino + " simulando el entorno original.");
    }
}