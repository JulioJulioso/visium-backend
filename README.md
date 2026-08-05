# VISIUM Backend

API REST para administración de ópticas (multi-empresa).  
Stack: **Spring Boot 4.1**, **Java 21**, **PostgreSQL**, **JWT**.

- Rama de trabajo: `desarrollo-backend`
- Versión actual: **0.2.0** (ver [`CHANGELOG.md`](CHANGELOG.md))
- Documentación de negocio: [`docs/reglas-negocio.md`](docs/reglas-negocio.md), [`docs/modelamiento-datos.md`](docs/modelamiento-datos.md)

## Requisitos locales

1. JDK 21
2. Docker con el contenedor `visium-postgres` (puerto `5432`)
3. Archivo `.env` en la raíz de este proyecto (no se sube a Git). Copia desde `.env-ejemplo`:

```env
DB_URL=jdbc:postgresql://localhost:5432/visium
DB_USERNAME=postgres
DB_PASSWORD=<tu-password>
JWT_SECRET=<clave-de-al-menos-32-caracteres>
```

## Arranque

```powershell
cd visium-backend
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`

## Tests automatizados (v0.2.0)

Comando:

```powershell
.\mvnw.cmd test
```

Requisitos para la suite completa: PostgreSQL en Docker + `.env` válido.  
Los tests de `AccesoService` y consultas clínicas **no** necesitan base de datos.

### Verificación consultas clínicas y agenda (2026-07-30)

Comandos ejecutados sin PostgreSQL:

```bash
bash ./mvnw -q -DskipTests compile
bash ./mvnw -q -Dtest=AccesoServiceTest,ConsultaServiceTest,ConsultaControllerSecurityTest,CitaServiceTest,CitaControllerSecurityTest test
```

| Clase | Tests | Resultado | Qué valida |
|---|---:|---|---|
| `ConsultaServiceTest` | 2 | OK | Cierre de cita `CONFIRMADA`, creación de consulta y cambio a `ATENDIDA`; rechazo de cita no confirmada |
| `ConsultaControllerSecurityTest` | 8 | OK | Solo `RECEPCIONISTA` puede cerrar cita; lecturas siguen limitadas a `SUPER_ADMIN`, `JEFE` y `PROFESIONAL` |
| `CitaServiceTest` | 3 | OK | Agenda de citas `CONFIRMADA` por profesional, rango inválido y profesional limitado a su propia agenda |
| `CitaControllerSecurityTest` | 5 | OK | Solo `SUPER_ADMIN`, `JEFE` y `PROFESIONAL` acceden a la agenda |
| `AccesoServiceTest` | 11 | OK | Aislamiento multi-empresa / multi-sucursal |

### Resultado de la última ejecución completa con PostgreSQL (2026-07-29)

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Clase | Tests | Resultado | Qué valida |
|---|---:|---|---|
| `BackendApplicationTests` | 1 | OK | Smoke: arranca el contexto Spring Boot completo contra PostgreSQL (`visium`), carga JPA/Security/JWT |
| `AccesoServiceTest` | 11 | OK | Aislamiento multi-empresa / multi-sucursal **sin BD** |

### Detalle de `AccesoServiceTest` (11 casos)

| # | Caso | Resultado esperado | Estado |
|---|---|---|---|
| 1 | `SUPER_ADMIN` accede a cualquier empresa | Permitido; listado sin filtro de empresas | OK |
| 2 | `JEFE` solo accede a sus empresas (A, B); rechaza empresa C | `ForbiddenException` en C | OK |
| 3 | Rol `JEFE` ve todas las sucursales aunque el JWT liste alguna | Sin filtro de sucursal | OK |
| 4 | `JEFE_SUCURSAL` solo ve sucursales asignadas | OK en A1; 403 en A2 | OK |
| 5 | `JEFE` ve todas las sucursales de su empresa | Sin filtro de sucursal | OK |
| 6 | `JEFE` multi-empresa sin header / sin contexto | `BadRequestException` | OK |
| 7 | Multi-empresa con parámetro o `EmpresaContext` | Resuelve la empresa indicada | OK |
| 8 | `JEFE` con una sola empresa | Se resuelve sola sin header | OK |
| 9 | `SUPER_ADMIN` sin empresa indicada | `BadRequestException` (pide `X-Empresa-Id` o `empresaId`) | OK |
| 10 | `exigirSuperAdmin` con usuario `JEFE` | `ForbiddenException` | OK |
| 11 | Sin usuario autenticado | `ForbiddenException` | OK |

### Detalle de `BackendApplicationTests` (1 caso)

| Caso | Resultado | Notas |
|---|---|---|
| `contextLoads` | OK (~5 s) | Conectó a `jdbc:postgresql://localhost:5432/visium`, inicializó Hibernate y el contexto de seguridad |

> **Importante:** no commitear `.env`. Si el smoke falla con “Failed to determine a suitable driver class”, revisa que `.env` tenga valores (no claves vacías) y que Docker `visium-postgres` esté *Running*.

## Cómo versionar

Ver [`CHANGELOG.md`](CHANGELOG.md). Convención: `0.2.x` = aislamiento real por roles/empresa en código.

## Pruebas de perfiles (Bruno / API) — v0.2.0

Colección: carpeta [`bruno/`](bruno/). Seed: [`scripts/seed-perfiles-demo.sql`](scripts/seed-perfiles-demo.sql).

| Email | Password | Rol |
|---|---|---|
| `super@visium.cl` | `admin123` | SUPER_ADMIN |
| `jefe@visium.cl` | `admin123` | JEFE (Demo + Norte) |
| `jsucursal@visium.cl` | `admin123` | JEFE_SUCURSAL (solo Casa Matriz) |

### Resultado verificación API (2026-07-29)

```
TOTAL: 10  PASS: 10  FAIL: 0
```

| Caso | Resultado |
|---|---|
| SUPER_ADMIN login | PASS |
| SUPER_ADMIN lista todas las empresas (≥2) | PASS |
| JEFE login con 2 empresas | PASS |
| JEFE ve Demo y Norte | PASS |
| JEFE lista 2 sucursales en Demo | PASS |
| JEFE lista sucursales en Norte con `X-Empresa-Id` | PASS |
| JEFE empresa ajena → 403 | PASS |
| JEFE_SUCURSAL login (1 sucursal) | PASS |
| JEFE_SUCURSAL solo Casa Matriz | PASS |
| JEFE_SUCURSAL pide Norte → 403 | PASS |


## Documentacion de la API se encuentra en swagger
```url
http://localhost:8080/swagger-ui/index.html
```
