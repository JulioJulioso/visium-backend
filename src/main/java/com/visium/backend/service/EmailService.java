package com.visium.backend.service;

import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Consulta;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.repository.RecetaOpticaRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RecetaPdfService pdfService;
    private final RecetaOpticaRepository recetaOpticaRepository;

    @Async
    @Transactional
    public void enviarRecetaAutomatica(UUID recetaId) {

        // 1. Buscamos la receta en la base de datos
        RecetaOptica receta = recetaOpticaRepository.findById(recetaId).orElse(null);
        if (receta == null) {
            return;
        }

        // 2. Extraemos los datos de forma limpia y por partes
        Consulta consulta = receta.getConsulta();
        Cita cita = consulta.getCita();
        Paciente paciente = cita.getPaciente();
        Sucursal sucursal = cita.getSucursal();
        Empresa empresa = sucursal.getEmpresa();

        // Si el paciente no tiene correo, abortamos
        if (paciente.getEmail() == null || paciente.getEmail().isBlank()) {
            return;
        }

        try {
            // 3. Generamos el PDF
            byte[] pdfBytes = pdfService.generarPdf(receta);

            // 4. Preparamos el correo
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(paciente.getEmail());
            helper.setFrom("estayjose3@gmail.com", empresa.getRazonSocial());

            if (empresa.getEmail() != null && !empresa.getEmail().isBlank()) {
                helper.setReplyTo(empresa.getEmail());
            }

            helper.setSubject("Tu Receta Óptica - " + empresa.getRazonSocial());

            String cuerpo = "Hola " + paciente.getNombre() + ",\n\n"
                    + "Aquí tiene su copia de la receta óptica generada en nuestra " + sucursal.getNombre() + ".\n\n"
                    + "Gracias por su preferencia.\n"
                    + empresa.getRazonSocial();

            helper.setText(cuerpo);

            String nombreArchivo = "Receta_" + paciente.getNumeroDocumento() + ".pdf";
            helper.addAttachment(nombreArchivo, new ByteArrayResource(pdfBytes));

            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando correo de receta: " + e.getMessage());
        }
    }

    public void enviarCorreoPrueba(String tuCorreo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(tuCorreo);
            helper.setFrom("estayjose3@gmail.com", "Óptica VISIUM");
            helper.setSubject("Prueba de Integración VISIUM");
            helper.setText("¡Felicidades! Tu backend en Spring Boot está enviando correos correctamente a través de Brevo.");

            mailSender.send(mensaje);
            System.out.println("¡Correo de prueba enviado con éxito!");
        } catch (Exception e) {
            System.err.println("Error enviando correo de prueba: " + e.getMessage());
        }
    }

    // ¡NUEVO MÉTODO AGREGADO PARA EL TEST UNIFICADO!
    public void enviarCorreoPruebaConPdf(String correoDestino, byte[] pdfBytes, String nombrePaciente, String nombreSucursal, String nombreEmpresa) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setFrom("estayjose3@gmail.com", nombreEmpresa);
            helper.setSubject("Tu Receta Óptica - " + nombreEmpresa);

            // ¡ESTE ES EL MENSAJE IDÉNTICO AL ORIGINAL!
            String cuerpo = "Hola " + nombrePaciente + ",\n\n"
                    + "Aquí tiene su copia de la receta óptica generada en nuestra " + nombreSucursal + ".\n\n"
                    + "Gracias por su preferencia.\n"
                    + nombreEmpresa;

            helper.setText(cuerpo);

            helper.addAttachment("Receta_Prueba.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(mensaje);
            System.out.println("¡Correo clon de prueba enviado con éxito a " + correoDestino + "!");
        } catch (Exception e) {
            System.err.println("Error enviando correo clon de prueba: " + e.getMessage());
        }
    }
}