package com.visium.backend.controller;

import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.RecetaOpticaRepository;
import com.visium.backend.service.RecetaOpticaService;
import com.visium.backend.service.RecetaPdfService;
import com.visium.backend.service.EmailService; // <-- 1. ESTA LÍNEA FALTABA

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private final EmailService emailService; // <-- 2. Y ESTA LÍNEA FALTABA

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'JEFE', 'PROFESIONAL')")
    @Operation(
            summary = "Historial de recetas de un paciente",
            description = "REQUIERE token JWT. Roles: SUPER_ADMIN, JEFE o PROFESIONAL. "
                    + "Devuelve las recetas opticas del paciente.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<RecetaOptica>> historialPorPaciente(@PathVariable UUID pacienteId) {
        List<RecetaOptica> historial = recetaOpticaRepository.findHistorialByPacienteId(pacienteId);
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
        RecetaOptica receta = recetaOpticaRepository.findById(id)
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
                    + "Registra una nueva receta optica.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<RecetaOptica> crearReceta(@RequestBody RecetaOptica receta) {
        RecetaOptica nuevaReceta = recetaOpticaService.guardarReceta(receta);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReceta);
    }

    @GetMapping("/test-email")
    @Operation(
            summary = "Probar envio de correo (endpoint de prueba)",
            description = "PUBLICO (no requiere token JWT). "
                    + "Envia un correo de prueba a una direccion fija. SOLO para desarrollo.")
    public ResponseEntity<String> probarEmail() {
        String tuCorreo = "cfritzsepulveda8@gmail.com";

        emailService.enviarCorreoPrueba(tuCorreo);

        return ResponseEntity.ok("Intento de envío de correo finalizado. Revisa la consola de Java y tu bandeja de entrada.");
    }
    // --------------------------------------------------------
    // ENDPOINT TEMPORAL PARA PROBAR EL DISEÑO DEL PDF
    // --------------------------------------------------------
    @GetMapping("/test-pdf")
    public ResponseEntity<byte[]> probarPdfEnMemoria() {
        // 1. Crear Empresa y Sucursal ficticias
        com.visium.backend.entity.Empresa empresa = new com.visium.backend.entity.Empresa();
        empresa.setRazonSocial("Óptica Visium Test");

        com.visium.backend.entity.Sucursal sucursal = new com.visium.backend.entity.Sucursal();
        sucursal.setNombre("Sucursal Centro");
        sucursal.setEmpresa(empresa);

        // 2. Crear Paciente ficticio
        com.visium.backend.entity.Paciente paciente = new com.visium.backend.entity.Paciente();
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setNumeroDocumento("12.345.678-9");
        paciente.setFechaNacimiento(java.time.LocalDate.of(1990, 5, 15));

        // 3. Crear Cita y Consulta ficticias
        com.visium.backend.entity.Cita cita = new com.visium.backend.entity.Cita();
        cita.setSucursal(sucursal);
        cita.setPaciente(paciente);

        com.visium.backend.entity.Consulta consulta = new com.visium.backend.entity.Consulta();
        consulta.setCita(cita);
        consulta.setDiagnostico("Miopía y Astigmatismo");

        // 4. Crear la Receta principal
        RecetaOptica receta = new RecetaOptica();
        receta.setConsulta(consulta);
        receta.setFechaEmision(java.time.LocalDate.now());
        receta.setAdicion(new java.math.BigDecimal("2.50"));
        receta.setDistanciaPupilar(new java.math.BigDecimal("62.5"));
        receta.setObservaciones("Paciente requiere uso de lentes para lectura y pantallas.");

        // 5. Agregar detalles de los ojos (OD y OI)
        com.visium.backend.entity.RecetaOpticaDetalle detalleOD = new com.visium.backend.entity.RecetaOpticaDetalle();
        detalleOD.setOjo(com.visium.backend.enums.Ojo.OD);
        detalleOD.setEsfera(new java.math.BigDecimal("-1.25"));
        detalleOD.setCilindro(new java.math.BigDecimal("-0.50"));
        detalleOD.setEje((short) 90);
        receta.agregarDetalle(detalleOD); // Helper que ya tienes en tu entidad

        com.visium.backend.entity.RecetaOpticaDetalle detalleOI = new com.visium.backend.entity.RecetaOpticaDetalle();
        detalleOI.setOjo(com.visium.backend.enums.Ojo.OI);
        detalleOI.setEsfera(new java.math.BigDecimal("-1.50"));
        detalleOI.setCilindro(new java.math.BigDecimal("-0.25"));
        detalleOI.setEje((short) 85);
        receta.agregarDetalle(detalleOI);

        // 6. Generar el PDF
        byte[] pdfBytes = pdfService.generarPdf(receta);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Receta_Prueba.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
