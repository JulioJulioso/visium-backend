
## Servicios ya implementados

URL base local: `http://localhost:8080`.

- **Autenticación** (`AuthController`)
  - `POST /auth/login`: inicia sesión y entrega el JWT.
  - `GET /auth/me`: devuelve los datos del usuario autenticado.

- **Empresas** (`EmpresaController`)
  - `GET /empresas/`, `GET /empresas/{id}`.
  - `POST /empresas`, `PUT /empresas/{id}`, `DELETE /empresas/{id}`.

- **Sucursales** (`SucursalController`)
  - `GET /sucursales`, `GET /sucursales/{id}`.
  - `POST /sucursales`, `PUT /sucursales/{id}`, `DELETE /sucursales/{id}`.

- **Profesionales** (`ProfesionalController`)
  - `GET /profesionales`, `GET /profesionales/{id}`.
  - `POST /profesionales`.
  - Pendiente: edición, baja lógica, asignación de sucursales y disponibilidad.

- **Pacientes** (`PacienteController`)
  - `GET /pacientes`, `GET /pacientes/{id}`.
  - `POST /pacientes`, `PUT /pacientes/{id}`, `DELETE /pacientes/{id}`.
  - Pendiente: búsqueda, paginación, historial clínico y baja lógica.

# Distribucion de requerimientos

# Sugerencias previas
 - Pueden copiar y pegar el arbol de archivos con la IA para darle contexto al Prompt, luego copiar y pegar el requerimiento que les toco, le piden ayuda a la IA para ordenarse y pulir el codigo para que se adecue al front-end.

 - ChatGPT esta ofrenciendo un mes gratuito de Plus donde pueden instalar colocar la extension Codex en VS code e iniciar sesion con su cuenta de chatgpt.

 - Cada integrante debe crear una rama nueva atraves de la rama main, idealmente el nombre de la rama debe ser el nombre del integrante

 - Ya que Jose y Julio son unos de los mas presentes, los requerimientos asignados estan mas relacionados asi tienen mejor coordinacion durante el dia.

 - Las pruebas que realicen en bruno pueden guardarlas en la carpeta bruno/ agregando una carpeta con sus nombres con sus archivos de prueba dentro Ejemplo: bruno/nombre/listar.bru

### Cristian — Recuperación de contraseña

- **Alta — Recuperación de contraseña:** crear los DTOs, el servicio y los métodos en `AuthController`. El primer endpoint recibe un correo y solicita al servicio enviar un código numérico; el segundo recibe correo, código y nueva contraseña. Guardar el código cifrado/hash, su vencimiento, intentos y estado de uso. Responder siempre con un mensaje genérico y nunca registrar el código ni la contraseña en logs.
- **Archivos a trabajar:**
  - Controlador: `controller/AuthController.java`.
  - Servicios: `service/AuthService.java`, `service/PasswordRecoveryService.java`.
  - DTOs: `dto/auth/PasswordRecoveryRequest.java`, `dto/auth/PasswordRecoveryConfirmRequest.java`.
  - Persistencia: `entity/PasswordRecoveryCode.java`, `repository/PasswordRecoveryCodeRepository.java`.
  - Front-end: `visium-react/src/api/authApi.js`, `visium-react/src/auth/tokenStorage.js`.
- **Instrucción adicional:** mantener públicas las rutas de recuperación en `SecurityConfig` y verificar que el flujo no asigne ni exponga roles. El JWT debe guardarse y eliminarse mediante `tokenStorage.js`.
- **Entrega verificable:** probar solicitud y confirmación de recuperación con código válido, vencido y usado. Requiere revisión por tratar credenciales.

### Catalina — Usuarios y recepcionistas

- **Media:** crear, editar, listar y activar/desactivar usuarios y recepcionistas. Validar empresa, sucursal y rol antes de guardar; nunca devolver contraseñas.
- **Archivos a trabajar:**
  - Controladores: `controller/UsuarioController.java`, `controller/RecepcionistaController.java`.
  - Servicios: `service/UsuarioService.java`, `service/RecepcionistaService.java`.
  - DTOs: `dto/usuario/UsuarioRequest.java`, `dto/usuario/UsuarioResponse.java`, `dto/recepcionista/RecepcionistaRequest.java`, `dto/recepcionista/RecepcionistaResponse.java`.
  - Repositorios existentes: `repository/UsuarioRepository.java`, `repository/UsuarioEmpresaRepository.java`, `repository/UsuarioSucursalRepository.java`.
  - Front-end: `visium-react/src/api/usuariosApi.js`.
- **Instrucción adicional:** alinear el selector y las reglas del front-end exclusivamente con `SUPER_ADMIN`, `JEFE`, `JEFE_SUCURSAL`, `RECEPCIONISTA` y `PROFESIONAL`; no usar roles inventados ni mostrar acciones no autorizadas.
- **Entrega verificable:** probar alta, edición, cambio de estado y asignación de rol/sucursal.

### Karina — Agenda, citas y profesionales

- **Alta — Citas:** crear DTOs `CitaRequest` y `CitaResponse`, el CRUD y las acciones confirmar, cancelar y reagendar. Antes de crear o mover una cita, consultar si el profesional ya tiene otra en ese horario. Al cambiar el estado, llamar a `EmailService` para encolar el correo correspondiente al paciente.
- **Media — Profesionales:** agregar edición, baja lógica y una consulta de profesionales disponibles por sucursal y fecha. No eliminar registros físicos; marcar el profesional como inactivo.
- **Archivos a trabajar:**
  - Controladores: `controller/CitaController.java`, `controller/ProfesionalController.java`.
  - Servicios: `service/CitaService.java`, `service/ProfesionalService.java`.
  - DTOs: `dto/cita/CitaRequest.java`, `dto/cita/CitaResponse.java`, `dto/cita/CitaReagendarRequest.java`, `dto/profesional/ProfesionalDisponibilidadResponse.java`.
  - Mapper: `mapper/CitaMapper.java`.
  - Repositorios: `repository/CitaRepository.java`, `repository/ProfesionalRepository.java`.
  - Front-end: `visium-react/src/api/citasApi.js`, `visium-react/src/api/profesionalesApi.js`.
- **Instrucción adicional:** reutilizar `Cita` y `CitaRepository`; no crear duplicados. Reemplazar el consumo de `citas.json` en Gestión de Citas por `citasApi.js`, enviando el JWT en cada solicitud.
- **Entrega verificable:** probar creación de cita, rechazo de un horario ocupado, confirmación, cancelación y reagendamiento; verificar que cada acción genere una notificación pendiente.

### Ayleen — Pacientes y dashboard

- **Media — Pacientes:** ampliar el listado para buscar por texto y devolver páginas. Agregar el historial que reúna consultas y recetas, e implementar baja lógica.
- **Alta — Dashboard:** devolver totales y próximas citas del usuario/sucursal autenticado usando la base de datos, no archivos JSON.
- **Archivos a trabajar:**
  - Controladores: `controller/PacienteController.java`, `controller/DashboardController.java`.
  - Servicios: `service/PacienteService.java`, `service/DashboardService.java`.
  - DTOs: `dto/paciente/PacienteHistorialResponse.java`, `dto/paciente/PacientePageResponse.java`, `dto/dashboard/DashboardResumenResponse.java`.
  - Repositorios: `repository/PacienteRepository.java`, `repository/ConsultaRepository.java`, `repository/RecetaOpticaRepository.java`.
  - Front-end: `visium-react/src/api/pacientesApi.js`, `visium-react/src/api/dashboardApi.js`.
- **Instrucción adicional:** reemplazar `fetch('/data/...')` y datos de `localStorage` de pacientes y Dashboard por sus módulos API. Enviar empresa/sucursal activa y respetar los roles que pueden ver cada pantalla.
- **Entrega verificable:** probar búsqueda paginada, historial y resumen para una sucursal autorizada.

### Matias — Consultas clínicas

- **Alta — Consultas:** crear DTOs y endpoints para guardar la atención de un paciente. Solo permitir cerrar una cita confirmada; al terminar, actualizar su estado a `ATENDIDA`. Reutilizar las entidades existentes y comprobar que el paciente/cita pertenezcan a la empresa autorizada.
- **Archivos a trabajar:**
  - Controlador y servicio: `controller/ConsultaController.java`, `service/ConsultaService.java`.
  - DTOs: `dto/consulta/ConsultaRequest.java`, `dto/consulta/ConsultaResponse.java`, `dto/consulta/CerrarCitaConsultaRequest.java`.
  - Mapper: `mapper/ConsultaMapper.java`.
  - Repositorio: `repository/ConsultaRepository.java`.
  - Front-end: `visium-react/src/api/consultasApi.js`.
- **Instrucción adicional:** reutilizar `Consulta` y `ConsultaRepository`; validar empresa/sucursal antes de consultar o cerrar una atención.
- **Entrega verificable:** probar una consulta que cierra una cita confirmada y cambia su estado a `ATENDIDA`.

### Jose — Recetas y documentos PDF

- **Alta — Recetas:** crear recetas y detalles, permitiendo borrador o emisión. Al emitir, generar el PDF y encolar el correo con el adjunto.
- **Media — PDF:** generar la receta óptica y la agenda desde plantillas HTML, validando acceso antes de descargar.
- **Archivos a trabajar:**
  - Controladores: `controller/RecetaOpticaController.java`, `controller/ReporteController.java`.
  - Servicios y PDF: `service/RecetaOpticaService.java`, `document/PdfService.java`, `document/RecetaPdfService.java`, `document/AgendaPdfService.java`.
  - DTOs: `dto/receta/RecetaOpticaRequest.java`, `dto/receta/RecetaOpticaResponse.java`, `dto/receta/RecetaOpticaDetalleRequest.java`, `dto/receta/RecetaOpticaDetalleResponse.java`.
  - Mappers: `mapper/RecetaOpticaMapper.java`, `mapper/RecetaOpticaDetalleMapper.java`.
  - Plantillas y repositorios: `templates/pdf/receta-optica.html`, `templates/pdf/agenda.html`, `repository/RecetaOpticaRepository.java`, `repository/RecetaOpticaDetalleRepository.java`.
  - Front-end: `visium-react/src/api/recetasApi.js`.
- **Instrucción adicional:** reutilizar `RecetaOptica`, `RecetaOpticaDetalle` y sus repositorios; no crear duplicados. Reemplazar los datos locales de recetas por solicitudes autenticadas mediante JWT.
- **Entrega verificable:** probar receta emitida, PDF descargable y correo pendiente con PDF adjunto.

### Julio — Solicitudes, soporte y correo electrónico

- **Alta — Solicitudes de demo:** crear DTO, entidad, repositorio, servicio y controlador siguiendo el patrón de los módulos existentes. Guardar los datos del formulario y encolar dos correos: acuse para quien lo solicitó y aviso para el equipo interno.
- **Media — Soporte:** crear el formulario de ticket con DTO, entidad, repositorio, servicio y controlador. Guardar el ticket antes de encolar el correo de aviso, para no perder la solicitud si falla el proveedor de email.
- **Transversal — Correo:** implementar `EmailService` y `SmtpEmailService` usando plantillas. Crear métodos para código de recuperación, demo, soporte, confirmación, cancelación, reagendamiento y receta con PDF. En esta primera etapa se debe poder registrar cada envío en `notificaciones_email`; el envío asíncrono puede revisarse con un integrante senior.
- **Archivos a trabajar:**
  - Controladores y servicios: `controller/SolicitudDemoController.java`, `controller/SoporteController.java`, `service/SolicitudDemoService.java`, `service/SoporteService.java`.
  - Correo: `notification/EmailMessage.java`, `notification/EmailService.java`, `notification/SmtpEmailService.java`, todos los archivos de `templates/email/`.
  - Entidades y repositorios: `entity/SolicitudDemo.java`, `entity/TicketSoporte.java`, `entity/NotificacionEmail.java`, `repository/SolicitudDemoRepository.java`, `repository/TicketSoporteRepository.java`, `repository/NotificacionEmailRepository.java`.
  - DTOs: `dto/solicitud/SolicitudDemoRequest.java`, `dto/solicitud/SolicitudDemoResponse.java`, `dto/soporte/ContactoSoporteRequest.java`, `dto/soporte/TicketSoporteResponse.java`.
  - Front-end: `visium-react/src/api/soporteApi.js`, `visium-react/src/api/solicitudesDemoApi.js`; coordinar la integración de `visium-react/src/api/httpClient.js` con el integrante 1.
- **Instrucción adicional:** implementar `httpClient.js` para centralizar la URL base, `Authorization: Bearer <JWT>` y errores `401`/`403`. Los correos y endpoints deben respetar el contexto de empresa y los roles reales; coordinar con Karina y Jose los eventos de cita y receta.
- **Entrega verificable:** probar que demo, soporte y una acción de cita crean su registro y una notificación de correo, incluso si el servidor SMTP está deshabilitado.

## Correo electrónico

Se requiere un módulo transversal para recuperación de contraseña mediante código,
solicitud de demo, soporte, notificaciones de citas y envío de recetas.

- Interfaces: `EmailService`, `EmailMessage` y plantillas por caso de uso.
- Operaciones internas: `sendPasswordRecoveryCode`, `sendDemoReceived`,
  `sendDemoNotification`, `sendSupportNotification`, `sendAppointmentConfirmed`,
  `sendAppointmentCancelled`, `sendAppointmentRescheduled` y `sendPrescription`
  (con PDF adjunto o enlace temporal autorizado).
- Recuperación: el correo debe mostrar únicamente el código, su vencimiento y una
  advertencia de seguridad; no incluir enlaces o contraseñas. Guardar solo el hash
  del código y limitar los intentos de validación.
- Citas: los tres correos son obligatorios. Los de confirmación, cancelación y
  reagendamiento deben indicar paciente, profesional, sucursal, fecha/hora y estado;
  el de reagendamiento debe incluir además la fecha/hora anterior y la nueva.
- Recetas: al emitirla, enviar un correo independiente al paciente mediante
  `sendPrescription`, con el PDF generado por `RecetaPdfService` adjunto. El archivo
  debe provenir de los datos persistidos y el envío debe quedar registrado en el
  outbox de notificaciones.
- Dependencia/configuración: agregar `spring-boot-starter-mail`; credenciales y
  remitente en variables de entorno (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`,
  `MAIL_PASSWORD`, `MAIL_FROM`), nunca en el repositorio.
- Para producción, registrar estado, intentos y errores en una tabla de cola/outbox
  (`notificaciones_email`) y enviarlos de manera asíncrona. La acción de negocio no
  debe perderse si el proveedor SMTP falla.



## Generación de PDF

Priorizar dos documentos: receta óptica emitida y agenda diaria/semanal. El PDF debe
generarse desde el servidor con los datos persistidos, no desde valores del DOM.

- Contrato: responder `application/pdf` con `Content-Disposition: attachment`;
  alternativamente guardar una referencia de archivo y entregar una URL temporal.
- Plantillas: `receta-optica` (paciente, profesional, empresa/sucursal, detalle OD/OI,
  indicaciones, fecha y folio) y `agenda` (fecha, sucursal, profesional, citas y
  estado). Incluir logo solo desde un recurso de confianza.
- Librería sugerida: OpenHTMLtoPDF + plantilla HTML, o Apache PDFBox para diseño
  programático. Incorporar pruebas que validen tipo MIME y contenido mínimo.
- Seguridad: validar acceso mediante `AccesoService`; no aceptar datos clínicos
  arbitrarios en el request para construir el documento.


Las entidades y repositorios de `Cita`, `Consulta`, `RecetaOptica` y
`RecetaOpticaDetalle` ya existen; faltan sus DTO, mappers, servicios y controladores.
Antes de implementar, alinear los roles visibles del front-end con los roles reales
del back-end (`SUPER_ADMIN`, `JEFE`, `JEFE_SUCURSAL`, `RECEPCIONISTA`,
`PROFESIONAL`) y reemplazar gradualmente cada `fetch('/data/...')`/`localStorage`
por un cliente HTTP autenticado con JWT.

## Árbol de archivos esperado

```text
visium-backend/
├── src/
│   └── main/
│       ├── java/com/visium/backend/
│       │   ├── BackendApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── MailProperties.java
│       │   │   └── PdfConfig.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── CitaController.java
│       │   │   ├── ConsultaController.java
│       │   │   ├── DashboardController.java
│       │   │   ├── EmpresaController.java
│       │   │   ├── PacienteController.java
│       │   │   ├── ProfesionalController.java
│       │   │   ├── RecepcionistaController.java
│       │   │   ├── RecetaOpticaController.java
│       │   │   ├── ReporteController.java
│       │   │   ├── SolicitudDemoController.java
│       │   │   ├── SoporteController.java
│       │   │   ├── SucursalController.java
│       │   │   └── UsuarioController.java
│       │   ├── dto/
│       │   │   ├── auth/
│       │   │   │   ├── LoginRequest.java, LoginResponse.java, MeResponse.java
│       │   │   │   ├── PasswordRecoveryRequest.java
│       │   │   │   └── PasswordRecoveryConfirmRequest.java  # correo, código y nueva contraseña
│       │   │   ├── cita/
│       │   │   │   ├── CitaRequest.java, CitaResponse.java
│       │   │   │   └── CitaReagendarRequest.java
│       │   │   ├── consulta/
│       │   │   │   ├── ConsultaRequest.java, ConsultaResponse.java
│       │   │   │   └── CerrarCitaConsultaRequest.java
│       │   │   ├── dashboard/
│       │   │   │   └── DashboardResumenResponse.java
│       │   │   ├── empresa/
│       │   │   │   └── EmpresaRequest.java, EmpresaResponse.java
│       │   │   ├── paciente/
│       │   │   │   ├── PacienteRequest.java, PacienteResponse.java
│       │   │   │   ├── PacienteHistorialResponse.java
│       │   │   │   └── PacientePageResponse.java
│       │   │   ├── profesional/
│       │   │   │   ├── ProfesionalRequest.java, ProfesionalResponse.java
│       │   │   │   └── ProfesionalDisponibilidadResponse.java
│       │   │   ├── receta/
│       │   │   │   ├── RecetaOpticaRequest.java, RecetaOpticaResponse.java
│       │   │   │   └── RecetaOpticaDetalleRequest.java, RecetaOpticaDetalleResponse.java
│       │   │   ├── recepcionista/
│       │   │   │   └── RecepcionistaRequest.java, RecepcionistaResponse.java
│       │   │   ├── solicitud/
│       │   │   │   └── SolicitudDemoRequest.java, SolicitudDemoResponse.java
│       │   │   ├── soporte/
│       │   │   │   └── ContactoSoporteRequest.java, TicketSoporteResponse.java
│       │   │   ├── sucursal/
│       │   │   │   └── SucursalRequest.java, SucursalResponse.java
│       │   │   └── usuario/
│       │   │       └── UsuarioRequest.java, UsuarioResponse.java
│       │   ├── entity/
│       │   │   ├── Cita.java
│       │   │   ├── Consulta.java
│       │   │   ├── Empresa.java
│       │   │   ├── EnfermedadSistemica.java
│       │   │   ├── FichaClinica.java
│       │   │   ├── NotificacionEmail.java
│       │   │   ├── Paciente.java
│       │   │   ├── PacienteEnfermedadSistemica.java
│       │   │   ├── PasswordRecoveryCode.java
│       │   │   ├── Profesional.java
│       │   │   ├── RecetaOptica.java
│       │   │   ├── RecetaOpticaDetalle.java
│       │   │   ├── Rol.java
│       │   │   ├── SolicitudDemo.java
│       │   │   ├── Sucursal.java
│       │   │   ├── TicketSoporte.java
│       │   │   ├── Usuario.java
│       │   │   ├── UsuarioEmpresa.java
│       │   │   ├── UsuarioEmpresaRol.java
│       │   │   └── UsuarioSucursal.java
│       │   ├── enums/
│       │   │   ├── EstadoCita.java
│       │   │   ├── Ojo.java
│       │   │   ├── Sexo.java
│       │   │   └── TipoDocumento.java
│       │   ├── exception/
│       │   │   ├── BadRequestException.java
│       │   │   ├── ForbiddenException.java
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   └── ResourceNotFoundException.java
│       │   ├── mapper/
│       │   │   ├── CitaMapper.java
│       │   │   ├── ConsultaMapper.java
│       │   │   ├── EmpresaMapper.java
│       │   │   ├── PacienteMapper.java
│       │   │   ├── RecetaOpticaDetalleMapper.java
│       │   │   ├── RecetaOpticaMapper.java
│       │   │   └── SucursalMapper.java
│       │   ├── notification/
│       │   │   ├── EmailMessage.java
│       │   │   ├── EmailService.java
│       │   │   └── SmtpEmailService.java
│       │   ├── document/
│       │   │   ├── AgendaPdfService.java
│       │   │   ├── PdfService.java
│       │   │   └── RecetaPdfService.java
│       │   ├── repository/
│       │   │   ├── CitaRepository.java
│       │   │   ├── ConsultaRepository.java
│       │   │   ├── EmpresaRepository.java
│       │   │   ├── EnfermedadSistemicaRepository.java
│       │   │   ├── FichaClinicaRepository.java
│       │   │   ├── NotificacionEmailRepository.java
│       │   │   ├── PacienteEnfermedadSistemicaRepository.java
│       │   │   ├── PacienteRepository.java
│       │   │   ├── PasswordRecoveryCodeRepository.java
│       │   │   ├── ProfesionalRepository.java
│       │   │   ├── RecetaOpticaDetalleRepository.java
│       │   │   ├── RecetaOpticaRepository.java
│       │   │   ├── RolRepository.java
│       │   │   ├── SolicitudDemoRepository.java
│       │   │   ├── SucursalRepository.java
│       │   │   ├── TicketSoporteRepository.java
│       │   │   ├── UsuarioEmpresaRepository.java
│       │   │   ├── UsuarioEmpresaRolRepository.java
│       │   │   ├── UsuarioRepository.java
│       │   │   └── UsuarioSucursalRepository.java
│       │   ├── security/
│       │   │   ├── EmpresaContext.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── JwtUtil.java
│       │   │   ├── UsuarioDetails.java
│       │   │   └── UsuarioDetailsService.java
│       │   └── service/
│       │       ├── AccesoService.java
│       │       ├── AuthService.java
│       │       ├── CitaService.java
│       │       ├── ConsultaService.java
│       │       ├── DashboardService.java
│       │       ├── EmpresaService.java
│       │       ├── PacienteService.java
│       │       ├── PasswordRecoveryService.java
│       │       ├── ProfesionalService.java
│       │       ├── RecepcionistaService.java
│       │       ├── RecetaOpticaService.java
│       │       ├── SolicitudDemoService.java
│       │       ├── SoporteService.java
│       │       ├── SucursalService.java
│       │       └── UsuarioService.java
│       └── resources/
│           └── templates/
│               ├── email/
│               │   ├── base.html
│               │   ├── password-recovery.html
│               │   ├── demo-received.html
│               │   ├── demo-notification.html
│               │   ├── support-notification.html
│               │   ├── appointment-confirmation.html
│               │   ├── appointment-cancellation.html
│               │   ├── appointment-rescheduling.html
│               │   └── prescription-issued.html
│               └── pdf/
│                   ├── receta-optica.html
│                   └── agenda.html
└── docs/
    └── servicios-pendientes-frontend.md
```

## Archivos propuestos para centralizar el consumo autenticado en el front-end

```text
visium-react/
└── src/
    ├── api/
    │   ├── httpClient.js                 # agrega Authorization: Bearer <JWT>
    │   ├── authApi.js
    │   ├── citasApi.js
    │   ├── consultasApi.js
    │   ├── dashboardApi.js
    │   ├── pacientesApi.js
    │   ├── profesionalesApi.js
    │   ├── recetasApi.js
    │   ├── soporteApi.js
    │   ├── solicitudesDemoApi.js
    │   └── usuariosApi.js
    └── auth/
        └── tokenStorage.js               # guarda, obtiene y elimina el JWT
```

## Guía breve de archivos

- `BackendApplication.java`: inicia la aplicación Spring Boot.
- `config/SecurityConfig.java`: define autenticación JWT, permisos y rutas públicas.
- `config/MailProperties.java`: reúne la configuración de correo desde variables de entorno.
- `config/PdfConfig.java`: configura el generador de documentos PDF.

- `controller/AuthController.java`: recibe login, recuperación y confirmación de contraseña.
- `controller/CitaController.java`, `ConsultaController.java` y `RecetaOpticaController.java`: exponen las operaciones HTTP de agenda, atención clínica y recetas.
- `controller/DashboardController.java` y `ReporteController.java`: entregan el resumen del dashboard y los PDF.
- `controller/EmpresaController.java`, `SucursalController.java`, `PacienteController.java` y `ProfesionalController.java`: administran los datos maestros ya existentes.
- `controller/UsuarioController.java` y `RecepcionistaController.java`: administran cuentas, roles y recepcionistas.
- `controller/SolicitudDemoController.java` y `SoporteController.java`: reciben formularios externos de demo y soporte.

- `dto/auth/*`: datos de login y recuperación; `PasswordRecoveryConfirmRequest` contiene correo, código y nueva contraseña.
- `dto/cita/*`: datos para crear, mostrar y reagendar citas.
- `dto/consulta/*`: datos de la atención clínica y del cierre de una cita.
- `dto/receta/*`: datos de receta óptica y sus detalles.
- `dto/dashboard/DashboardResumenResponse.java`: respuesta de totales y próximas citas.
- `dto/empresa/*`, `dto/sucursal/*`, `dto/paciente/*` y `dto/profesional/*`: entradas y salidas de sus módulos; los DTO de paciente incluyen historial y paginación, y el de profesional incluye disponibilidad.
- `dto/usuario/*` y `dto/recepcionista/*`: entradas y salidas administrativas, sin contraseñas.
- `dto/solicitud/*` y `dto/soporte/*`: datos del formulario de demo y del ticket de soporte.

- `entity/Cita.java`, `Consulta.java`, `RecetaOptica.java` y `RecetaOpticaDetalle.java`: representan agenda, atención y receta en la base de datos.
- `entity/Empresa.java`, `Sucursal.java`, `Paciente.java`, `Profesional.java`, `Usuario.java` y `Rol.java`: representan los datos principales del sistema.
- `entity/FichaClinica.java`, `EnfermedadSistemica.java` y `PacienteEnfermedadSistemica.java`: guardan antecedentes clínicos del paciente.
- `entity/UsuarioEmpresa.java`, `UsuarioEmpresaRol.java` y `UsuarioSucursal.java`: relacionan usuarios con empresas, roles y sucursales.
- `entity/PasswordRecoveryCode.java`: guarda de forma segura el código de recuperación, vencimiento, intentos y estado de uso.
- `entity/SolicitudDemo.java`, `TicketSoporte.java` y `NotificacionEmail.java`: persisten solicitudes, tickets y correos pendientes de envío.

- `repository/*Repository.java`: cada repositorio consulta y guarda la entidad del mismo nombre; `PasswordRecoveryCodeRepository` busca y actualiza códigos, y `NotificacionEmailRepository` gestiona el outbox de correos.
- `mapper/EmpresaMapper.java`, `PacienteMapper.java` y `SucursalMapper.java`: convierten entidades a DTOs y viceversa.
- `enums/EstadoCita.java`, `Ojo.java`, `Sexo.java` y `TipoDocumento.java`: restringen valores válidos del dominio.
- `exception/BadRequestException.java`, `ForbiddenException.java` y `ResourceNotFoundException.java`: representan errores de validación, permisos y recursos inexistentes.
- `exception/GlobalExceptionHandler.java`: transforma errores en respuestas HTTP uniformes.

- `security/JwtUtil.java`: crea y valida tokens JWT.
- `security/JwtAuthenticationFilter.java`: lee el token de cada solicitud protegida.
- `security/UsuarioDetails.java` y `UsuarioDetailsService.java`: adaptan el usuario de la base de datos a Spring Security.
- `security/EmpresaContext.java`: conserva la empresa activa durante la solicitud.

- `service/AccesoService.java`: verifica el alcance de empresa, sucursal y permisos.
- `service/AuthService.java` y `PasswordRecoveryService.java`: gestionan autenticación y recuperación con código.
- `service/CitaService.java`, `ConsultaService.java` y `RecetaOpticaService.java`: contienen las reglas de negocio de citas, consultas y recetas.
- `service/DashboardService.java`: prepara los datos de resumen; los reportes PDF se delegan desde `ReporteController.java` a los servicios de `document/`.
- `service/EmpresaService.java`, `SucursalService.java`, `PacienteService.java` y `ProfesionalService.java`: contienen la lógica de sus módulos maestros.
- `service/UsuarioService.java`, `RecepcionistaService.java`, `SolicitudDemoService.java` y `SoporteService.java`: implementan las reglas administrativas y de formularios.

- `notification/EmailMessage.java`: representa un correo a enviar.
- `notification/EmailService.java`: contrato para enviar o encolar correos.
- `notification/SmtpEmailService.java`: implementación que usa SMTP y las plantillas HTML.
- `document/PdfService.java`: contrato común para documentos PDF.
- `document/RecetaPdfService.java` y `AgendaPdfService.java`: generan los PDF de receta y agenda.

- `templates/email/base.html`: estructura visual reutilizable de los correos.
- `templates/email/password-recovery.html`: correo con código y vencimiento de recuperación.
- `templates/email/demo-received.html` y `demo-notification.html`: acuse al solicitante y aviso interno de demo.
- `templates/email/support-notification.html`: aviso interno de un ticket de soporte.
- `templates/email/appointment-confirmation.html`, `appointment-cancellation.html` y `appointment-rescheduling.html`: avisos al paciente por cada cambio de una cita.
- `templates/email/prescription-issued.html`: correo al paciente con la receta PDF adjunta.
- `templates/pdf/receta-optica.html` y `agenda.html`: plantillas HTML usadas para construir los PDF.
- `docs/servicios-pendientes-frontend.md`: documento de alcance, distribución de tareas y rutas previstas.

## Endpoints por página del front-end

URL base local: `http://localhost:8080`. El proyecto no configura un puerto ni un
context path, por lo que Spring Boot utilizará el puerto `8080` por defecto. Las
rutas indicadas son la ubicación prevista de la implementación en el back-end.

- Página **Recuperar contraseña**
  - URL: `POST http://localhost:8080/auth/password-recovery`
  - Ruta: `controller/AuthController.java` → `service/PasswordRecoveryService.java`
  - URL: `POST http://localhost:8080/auth/password-recovery/confirm`
  - Ruta: `controller/AuthController.java` → `service/PasswordRecoveryService.java`

- Página **Solicitar demo**
  - URL: `POST http://localhost:8080/solicitudes-demo`
  - Ruta: `controller/SolicitudDemoController.java` → `service/SolicitudDemoService.java`

- Página **Gestión de Citas** — crear y administrar citas
  - URL: CRUD `http://localhost:8080/citas`
  - URL: `PATCH http://localhost:8080/citas/{id}/confirmar`
  - URL: `PATCH http://localhost:8080/citas/{id}/cancelar`
  - URL: `PATCH http://localhost:8080/citas/{id}/reagendar`
  - Ruta: `controller/CitaController.java` → `service/CitaService.java` →
    `notification/EmailService.java`.

- Página **Ficha de paciente / Historial clínico** — consultas
  - URL: CRUD `http://localhost:8080/consultas`
  - URL: `POST http://localhost:8080/citas/{id}/consulta`
  - Ruta: `controller/ConsultaController.java` → `service/ConsultaService.java`

- Página **Nueva receta / Historial de recetas**
  - URL: CRUD `http://localhost:8080/recetas`
  - URL: CRUD `http://localhost:8080/recetas/{id}/detalles`
  - URL: `GET http://localhost:8080/recetas/{id}/pdf`
  - Ruta: `controller/RecetaOpticaController.java` → `service/RecetaOpticaService.java` →
    `document/RecetaPdfService.java`.

- Página **Dashboard**
  - URL: `GET http://localhost:8080/dashboard/resumen`
  - Ruta: `controller/DashboardController.java` → `service/DashboardService.java`

- Página **Gestión administrativa — Usuarios y recepcionistas**
  - URL: CRUD `http://localhost:8080/usuarios`
  - URL: CRUD `http://localhost:8080/recepcionistas`
  - Ruta: `controller/UsuarioController.java` → `service/UsuarioService.java`; y
    `controller/RecepcionistaController.java` → `service/RecepcionistaService.java`.

- Página **Gestión administrativa — Profesionales**
  - URL: `PUT http://localhost:8080/profesionales/{id}`
  - URL: `DELETE http://localhost:8080/profesionales/{id}`
  - URL: `GET http://localhost:8080/profesionales/disponibles?sucursalId={id}&fecha={fecha}`
  - Ruta: `controller/ProfesionalController.java` → `service/ProfesionalService.java`

- Página **Gestión de pacientes / Nuevo paciente / Buscador del encabezado**
  - URL: `GET http://localhost:8080/pacientes?empresaId={id}&q={texto}&page={n}&size={n}`
  - Ruta: `controller/PacienteController.java` → `service/PacienteService.java`

- Página **Gestión de Citas** — imprimir agenda diaria o semanal
  - URL: `GET http://localhost:8080/reportes/agenda.pdf`
  - Ruta: `controller/ReporteController.java` → `document/AgendaPdfService.java`

- Página **Contacto / Soporte**
  - URL: `POST http://localhost:8080/soporte/contactos`
  - Ruta: `controller/SoporteController.java` → `service/SoporteService.java` →
    `notification/EmailService.java`.
