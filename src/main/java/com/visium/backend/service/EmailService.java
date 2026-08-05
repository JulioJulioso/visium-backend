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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j // Activa el sistema de logs profesional
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RecetaPdfService pdfService;
    private final RecetaOpticaRepository recetaOpticaRepository;

    // Inyecta dinámicamente tu correo desde application.yaml (o usa estayjose3@gmail.com por defecto)
    @Value("${spring.mail.username:estayjose3@gmail.com}")
    private String correoRemitente;

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
            // Usamos la variable inyectada en lugar de tener el correo hardcodeado
            helper.setFrom(correoRemitente, empresa.getRazonSocial());

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
            log.info("Receta enviada automáticamente con éxito a {}", paciente.getEmail());

        } catch (Exception e) {
            log.error("Error enviando correo de receta: {}", e.getMessage(), e);
        }
    }

    public void enviarCorreoPrueba(String tuCorreo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(tuCorreo); // Correo de destino
            helper.setFrom(correoRemitente, "Óptica VISIUM");
            helper.setSubject("Prueba de Integración VISIUM");
            helper.setText("¡Felicidades! Tu backend en Spring Boot está enviando correos correctamente a través de Brevo.");

            mailSender.send(mensaje);
            log.info("¡Correo de prueba enviado con éxito a {}!", tuCorreo);

        } catch (Exception e) {
            log.error("Error enviando correo de prueba: {}", e.getMessage(), e);
        }
    }

    public void enviarCorreoPruebaConPdf(String tuCorreoDestino, byte[] pdfBytes, String razonSocial) {
        try {
            // 1. Preparamos el correo
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            // 2. Configuramos destinatario y remitente
            helper.setTo(tuCorreoDestino);
            helper.setFrom(correoRemitente, razonSocial);
            helper.setSubject("Prueba de Receta PDF - " + razonSocial);

            // 3. Escribimos el cuerpo del correo
            String cuerpo = "¡Hola!\n\nEste es un correo de prueba unificado. "
                    + "Si estás leyendo esto y puedes abrir el PDF adjunto, significa que tu generador "
                    + "de recetas y la conexión con Brevo están funcionando a la perfección.\n\nSaludos!";
            helper.setText(cuerpo);

            // 4. Adjuntamos el PDF (convertimos los bytes crudos a un archivo adjunto)
            helper.addAttachment("Receta_Prueba.pdf", new org.springframework.core.io.ByteArrayResource(pdfBytes));

            // 5. ¡Enviamos!
            mailSender.send(mensaje);
            log.info("¡Correo de prueba con PDF adjunto enviado con éxito a {}!", tuCorreoDestino);

        } catch (Exception e) {
            log.error("Error enviando correo de prueba con PDF: {}", e.getMessage(), e);
        }
    }

    /** Envia el codigo sin registrarlo en logs ni incluir datos de autenticacion. */
    public void enviarCodigoRecuperacion(String email, String code) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setTo(email);
            helper.setFrom("onboarding@resend.dev", "Óptica VISIUM");
            helper.setSubject("Código de recuperación de contraseña");
            helper.setText("Tu código de recuperación es: " + code
                    + "\n\nVence en 15 minutos. Si no solicitaste este cambio, ignora este correo.");
            mailSender.send(mensaje);
        } catch (Exception ignored) {
            // La respuesta debe seguir siendo generica para no filtrar cuentas ni secretos.
        }
    }
}
