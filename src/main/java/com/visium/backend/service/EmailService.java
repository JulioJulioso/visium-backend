package com.visium.backend.service;

// 1. IMPORTAMOS TODAS LAS CLASES PARA QUE TU EDITOR NO MARQUE ERROR
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
    // Agregamos el repositorio para buscar la receta fresca
    private final RecetaOpticaRepository recetaOpticaRepository;

    @Async
    @Transactional // Mantiene viva la conexión a PostgreSQL en este hilo
    public void enviarRecetaAutomatica(UUID recetaId) { // Ahora recibe el ID

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
            helper.setFrom("onboarding@resend.dev", empresa.getRazonSocial());

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

            helper.setTo(tuCorreo); // DEBE ser tu correo registrado en Resend
            helper.setFrom("onboarding@resend.dev", "Óptica VISIUM");
            helper.setSubject("Prueba de Integración VISIUM");
            helper.setText("¡Felicidades! Tu backend en Spring Boot está enviando correos correctamente a través de Resend.");

            mailSender.send(mensaje);
            System.out.println("¡Correo de prueba enviado con éxito!");
        } catch (Exception e) {
            System.err.println("Error enviando correo de prueba: " + e.getMessage());
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
