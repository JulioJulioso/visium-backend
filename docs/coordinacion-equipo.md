
## Archivos compartidos

### Seguridad y JWT

- **Responsable:** Cristian.
- **Participan:** Catalina, Karina, Ayleen, Matias, Jose y Julio.
- **Archivos:** `config/SecurityConfig.java`, `api/httpClient.js`, `auth/tokenStorage.js`.
- **Propósito:** rutas públicas, permisos, token JWT y errores `401`/`403`.

### Notificaciones por correo

- **Responsable:** Julio.
- **Participan:** Cristian, Karina y Jose.
- **Archivos:** `notification/EmailService.java`, `EmailMessage.java`, `SmtpEmailService.java`, `templates/email/*`.
- **Propósito:** recuperación de contraseña, avisos de citas y receta con PDF adjunto.

### Outbox de correos

- **Responsable:** Julio.
- **Participan:** Karina y Jose.
- **Archivos:** `entity/NotificacionEmail.java`, `repository/NotificacionEmailRepository.java`.
- **Propósito:** registrar y encolar avisos de citas y recetas.

### Citas

- **Responsable:** Karina.
- **Participan:** Matias y Ayleen.
- **Archivos:** `entity/Cita.java`, `repository/CitaRepository.java`.
- **Propósito:** agenda, cierre de consulta e indicadores del dashboard.

### Consultas

- **Responsable:** Matias.
- **Participan:** Ayleen y Jose.
- **Archivos:** `entity/Consulta.java`, `repository/ConsultaRepository.java`.
- **Propósito:** historial del paciente y datos clínicos relacionados.

### Recetas

- **Responsable:** Jose.
- **Participa:** Ayleen.
- **Archivos:** `entity/RecetaOptica.java`, `RecetaOpticaDetalle.java` y sus repositorios.
- **Propósito:** historial de paciente y generación de PDF.

### Pacientes

- **Responsable:** Ayleen.
- **Participan:** Karina, Matias y Jose.
- **Archivos:** `entity/Paciente.java`, `repository/PacienteRepository.java`.
- **Propósito:** validación del paciente en citas, consultas y recetas.

### PDF y correo de receta

- **Responsable:** Jose.
- **Participa:** Julio.
- **Archivo:** `document/RecetaPdfService.java`.
- **Propósito:** generar el archivo que se adjunta al correo del paciente.

## Módulos por integrante

### Cristian — Autenticación

`AuthController`, `AuthService`, `PasswordRecoveryService`, `dto/auth/*`, código de recuperación y `authApi.js`.

### Catalina — Usuarios

`UsuarioController`, `RecepcionistaController`, servicios, DTOs y `usuariosApi.js`.

### Karina — Agenda

`CitaController`, `ProfesionalController`, servicios, `CitaMapper`, DTOs y APIs de citas/profesionales.

### Ayleen — Pacientes

Controladores y servicios de pacientes/dashboard, DTOs de historial/paginación y APIs de pacientes/dashboard.

### Matias — Consultas

`ConsultaController`, `ConsultaService`, `ConsultaMapper`, DTOs y `consultasApi.js`.

### Jose — Recetas y PDF

Controladores, servicios, mappers, DTOs, servicios PDF, plantillas PDF y `recetasApi.js`.

### Julio — Demo, soporte y correo

Controladores, servicios, DTOs y entidades de demo/soporte; módulos de correo, plantillas y APIs correspondientes.

## Cómo coordinar las ramas y evitar conflictos

Cada integrante trabaja en una rama distinta de `main`. Antes de empezar, avisar al
equipo qué módulo y archivos se trabajarán, especialmente cuando sean compartidos.

1. **Crear una rama por tarea:** por ejemplo, `feature/citas-karina` o `feature/recetas-jose`. No subir cambios directamente a `main`.
2. **Evitar editar archivos de otra persona:** si se necesita un archivo compartido, avisar al responsable principal y acordar quién hará el cambio. Si ambos deben intervenir, separar el trabajo en secciones o commits distintos.
3. **Hacer commits pequeños y descriptivos:** separar DTOs, servicio/controlador y pruebas. No mezclar cambios de formato con cambios funcionales.
4. **Actualizar la rama antes de abrir el pull request:** incorporar los últimos cambios de `main`, resolver conflictos localmente y ejecutar las pruebas del módulo.
5. **Abrir un pull request completo:** indicar archivos modificados, endpoint probado y dependencias con otros integrantes. Solicitar revisión al responsable de cada archivo compartido.
6. **Integrar por dependencias:** primero autenticación y cliente HTTP; después citas, consultas y recetas; finalmente correo, PDF y pantallas dependientes.

**Regla clave:** si Git marca conflicto en una entidad, repositorio, DTO o servicio
compartido, no aceptar cambios automáticamente. Comparar ambos cambios con el
responsable del archivo y acordar una única versión.

## Reglas rápidas

- Avisar al responsable antes de modificar un archivo compartido.
- No cambiar DTOs, entidades o contratos de otro módulo sin acordarlo.
- Probar flujos de integración: cita + correo; receta + PDF + correo.
