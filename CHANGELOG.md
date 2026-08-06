# Changelog

Todos los cambios notables de Visium Backend se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

### Added
- Profesionales sin usuario: ahora todos los profesionales del seed pueden aparecer en `GET /profesionales` (38 profesionales con usuario asociado en BD)
- `POST /consultas/cerrar-cita` ahora permitido para `SUPER_ADMIN`, `JEFE` y `PROFESIONAL` (antes solo `RECEPCIONISTA`), según ajuste de regla de negocio
- `POST /recetas`: acepta `RecetaRequest` (DTO con `consulta` como UUID) en vez de la entidad; valida que la consulta exista y no tenga receta previa (400); responde `RecetaResponse` (201)
- `GET /recetas/paciente/{id}`: devuelve historial como `RecetaResponse` (DTO plano, sin entidades)

### Corregido
- `RecetaOpticaRepository`: queries con `JOIN FETCH` (consulta, cita, paciente, sucursal, empresa, profesional, detalles) para evitar `LazyInitializationException` al serializar historial y generar el PDF (`open-in-view: false`)
- `GET /recetas/{id}/pdf`: arreglado el 500 "Error al generar el PDF" por acceso a `sucursal.empresa` (LAZY) fuera de transacción
- `CitaService`: validaciones contra profesionales sin usuario lanzan 400/403 con mensaje claro en vez de NPE (500)
- `ConsultaService.cerrarCita`: `fechaInicio` y `fechaFin` se fijan con el mismo instante para no violar `ck_consultas_fechas` (antes `fecha_fin` podía quedar antes que `fecha_inicio` por la carrera entre `@PrePersist` y el servicio, generando 500)
- `Consulta.alCrear`: guard defensivo que corrige `fechaFin < fechaInicio` si vuelve a ocurrir una desincronización de reloj
- `CitaService.listarCitas`: cuando el rol ve todas las sucursales (lista vacía) se pasa `null` al filtro de la query (antes una lista vacía generaba `IN ()` y devolvía cero citas en `GET /citas`)

### Aislamiento multi-empresa al crear/modificar citas
- `modificarCita` valida acceso a la empresa nueva al cambiar `empresaId` (antes solo validaba la empresa original)
- Regla `fin > inicio` en crear y modificar citas
- Máquina de estados: `PENDIENTE → CONFIRMADA/CANCELADA`, `CONFIRMADA → PENDIENTE/CANCELADA`, `CANCELADA → PENDIENTE`; `ATENDIDA` y `NO_ASISTIO` son terminales (no modificables)
- Una cita nueva solo puede crearse como `PENDIENTE`
- `eliminarCita` rechaza borrar citas con consulta registrada (protege la FK de `consultas`; antes derivaba en error de integridad 500)
- `CitaMapper.toResponse` tolera relaciones nulas
- `GET /empresas` ya no exige barra final (Spring 6 no tolera trailing slash)
- `GET /pacientes` y `GET /sucursales` ya no exigen `empresaId` como query param obligatorio: la empresa se resuelve por `X-Empresa-Id` (o única empresa), igual que `GET /citas`
- `ProfesionalService` tolera profesionales sin `usuario` asociado (dato corrupto): los omite del listado en vez de lanzar NPE
- `GlobalExceptionHandler` ahora cubre excepciones no controladas (`MissingServletRequestParameterException` → 400, `MethodArgumentTypeMismatchException` → 400, `HttpRequestMethodNotSupportedException` → 405, `NoResourceFoundException` → 404, `Exception` → 500). Antes derivaban al endpoint `/error`, que la cadena de seguridad respondía como 401 "No autenticado" y el frontend lo interpretaba como sesión expirada (cerraba sesión al entrar a Gestión de Citas)
- Cierre del hueco de seguridad: `/profesionales/sucursal/**` pasó de `permitAll` a requerir autenticación

### Planeado
- CRUD de consultas y recetas
- Dashboard

---

## [0.2.0] - 2026-07-29

Aislamiento multi-empresa / multi-sucursal en código (rama `desarrollo-backend`).
Versión del artefacto: `0.2.0-SNAPSHOT`.

### Added
- Claims JWT: `roles`, `empresaIds`, `sucursalIds`
- `POST /auth/login` y `GET /auth/me` devuelven `empresaIds`, `sucursalIds`, `empresaActivaId`
- Header `X-Empresa-Id` + `EmpresaContext` (empresa activa por request)
- `AccesoService`: reglas de acceso por rol
  - `SUPER_ADMIN` → cualquier empresa
  - `JEFE` → solo sus empresas; todas las sucursales de cada una
  - `JEFE_SUCURSAL` (y roles con sucursales asignadas) → solo `usuarios_sucursales`
- `ForbiddenException` (HTTP 403) en el handler global
- Filtros de aislamiento en `EmpresaService`, `SucursalService`, `PacienteService`, `ProfesionalService`
- Tests unitarios de `AccesoService` (sin BD): `.\mvnw.cmd test`
- Smoke `BackendApplicationTests` (contexto Spring + PostgreSQL vía `.env` / Docker)
- Colección Bruno en `bruno/` + seed `scripts/seed-perfiles-demo.sql`
- Verificación API de 3 perfiles (2026-07-29): **10/10 PASS**

### Changed
- Crear / desactivar empresa restringido a `SUPER_ADMIN` (lógica de negocio)
- Crear / desactivar sucursales y registrar profesionales: `JEFE` o `SUPER_ADMIN`
- `@PreAuthorize` actualizado: ya no usa `ADMIN`
  - Empresas: crear/desactivar → `SUPER_ADMIN`; actualizar → `SUPER_ADMIN` | `JEFE`
  - Sucursales / profesionales (mutaciones) → `SUPER_ADMIN` | `JEFE`
  - Pacientes alta/edición → `SUPER_ADMIN` | `JEFE` | `JEFE_SUCURSAL` | `RECEPCIONISTA`
  - Pacientes baja → `SUPER_ADMIN` | `JEFE` | `JEFE_SUCURSAL`
- Documentación: `docs/reglas-negocio.md`, `docs/PLAN-DE-TRABAJO.md`, `README.md`

### Removed
- Rol legacy `ADMIN` (migración en `data.sql`: asignaciones → `JEFE`, luego delete del rol)

### Security
- Un request no puede leer ni mutar datos de empresa/sucursal ajena (403)
- Autorización en controllers alineada con la matriz de roles del producto
- Confirmado por API: SUPER_ADMIN ve todo; JEFE multi-empresa no cruza sin header; JEFE_SUCURSAL solo Casa Matriz

---

## [0.1.0] - 2026-07-28

Primera versión versionada del backend VISIUM (rama `desarrollo-backend`).

### Added
- Proyecto Spring Boot 4.1 + Java 21 + PostgreSQL + JWT
- Modelo de datos multi-empresa (entidades JPA y repositorios)
- Autenticación: `POST /auth/login`, `GET /auth/me`
- CRUD empresas, sucursales, pacientes
- Registro transaccional de profesionales (5 pasos de reglas de negocio)
- Seguridad con roles y CORS para el frontend (`localhost:5173`)
- Documentación: `docs/modelamiento-datos.md`, `docs/reglas-negocio.md`, `docs/PLAN-DE-TRABAJO.md`
- Roles documentados y seed en `data.sql`:
  - `SUPER_ADMIN` — dueños Visium (plataforma)
  - `JEFE` — dueño de una o varias ópticas
  - `JEFE_SUCURSAL` — jefe de una sucursal
  - `RECEPCIONISTA`, `PROFESIONAL`
- `ADMIN` se mantiene temporalmente como legacy hasta migrar los controllers
- Configuración por variables de entorno (`.env-ejemplo`)

### Changed
- Aislamiento por diseño: una sola base de datos filtrada por `empresa_id` (no una BD por óptica)
- `ddl-auto: update` + carga de `data.sql` al arrancar

### Security
- Secretos fuera del código (`DB_*`, `JWT_SECRET` vía `.env`)
- Endpoints protegidos por JWT salvo `/auth/login`

---

## Cómo versionar de aquí en adelante

1. Antes de cada commit relevante, actualiza la sección `[Unreleased]` o crea una versión nueva `## [X.Y.Z] - AAAA-MM-DD`.
2. Sube la versión en `pom.xml` (`<version>`) para que coincida.
3. Tipos de cambio:
   - **Added** — algo nuevo
   - **Changed** — cambio en lo existente
   - **Deprecated** — pronto se quita
   - **Removed** — eliminado
   - **Fixed** — corrección de bugs
   - **Security** — seguridad

### Convención de números
- `0.1.x` — base del MVP (auth, admin, pacientes, roles en docs)
- `0.2.x` — aislamiento real SUPER_ADMIN / JEFE / JEFE_SUCURSAL en código
- `0.3.x` — flujo clínico (citas, consultas, recetas)
- `1.0.0` — MVP listo para clientes

[Unreleased]: https://github.com/JulioJulioso/visium-backend/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/JulioJulioso/visium-backend/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/JulioJulioso/visium-backend/releases/tag/v0.1.0
