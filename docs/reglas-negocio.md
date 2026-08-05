# Reglas de negocio

## Usuarios y roles

- Un usuario puede pertenecer a una o varias empresas.
- Un usuario puede tener varios roles dentro de una empresa.
- Los roles deben verificarse en Spring Security.
- El rol no reemplaza la información específica de un profesional.
- Hay **una sola base de datos**; el aislamiento es por `empresa_id` (y sucursal cuando aplique).

### Roles del sistema

| Código | Quién | Alcance |
|---|---|---|
| `SUPER_ADMIN` | Dueños de Visium (plataforma) | Puede entrar a **cualquier** empresa/sucursal para soporte. Puede desactivar empresas o sucursales (ej. no pagan). No es el dueño operativo de las ópticas clientes. |
| `JEFE` | Dueño de una o varias ópticas (reemplaza al antiguo `ADMIN`) | Solo las empresas **suyas** (`usuarios_empresas`). Dentro de cada una ve **todas** sus sucursales. Si tiene Óptica A y B, él ve ambas; el personal de A no ve B. No puede tocar empresas ajenas. |
| `JEFE_SUCURSAL` | Jefe de una sucursal | Trabaja para una empresa, pero solo ve/gestiona sucursales asignadas en `usuarios_sucursales`. |
| `RECEPCIONISTA` | Operación | Su empresa (pacientes, citas). No administra personal ni sucursales. |
| `PROFESIONAL` | Clínico | Su empresa + sucursales asignadas. Consultas y recetas. |

### Diferencia SUPER_ADMIN vs JEFE

- `SUPER_ADMIN` = dueños de **Visium**. Poder global de soporte y corte de servicio.
- `JEFE` = dueño de **ópticas clientes**. Poder solo sobre empresas donde tiene pertenencia.

### Matriz de permisos (resumen)

| Acción | SUPER_ADMIN | JEFE | JEFE_SUCURSAL | RECEPCIONISTA | PROFESIONAL |
|---|---|---|---|---|---|
| Ver / gestionar cualquier empresa | Sí | No | No | No | No |
| Cortar servicio (empresa/sucursal no paga) | Sí | No | No | No | No |
| Gestionar sus empresas | — | Sí | No | No | No |
| Ver todas las sucursales de su empresa | Sí | Sí | No | No | No |
| Ver solo sucursales asignadas | — | — | Sí | Si aplica | Si aplica |
| Contratar / despedir / editar roles en su empresa | Sí | Sí | Limitado* | No | No |
| Pacientes / citas | Soporte | Sí | Sí (su alcance) | Sí | Ver / atender |
| Consultas / recetas | Soporte | Ver | Ver | Cierre operativo de cita | Sí |

\*Un `JEFE_SUCURSAL` puede gestionar personal de **su** sucursal si el producto lo habilita; no administra toda la óptica.

### Multi-empresa (dueño de varias ópticas)

- Un `JEFE` puede tener varias filas en `usuarios_empresas`.
- Debe trabajar con una **empresa activa** por request (header `X-Empresa-Id` cuando tenga más de una).
- Si solo tiene una empresa, el backend la toma como activa sin header.
- `POST /auth/login` y `GET /auth/me` devuelven `empresaIds`, `sucursalIds` y `empresaActivaId` (sugerida o la del header).
- El JWT incluye claims `roles`, `empresaIds` y `sucursalIds`.
- Los datos de la empresa A nunca se mezclan con los de la empresa B para recepcionistas, profesionales ni jefes de sucursal.
- En código: `AccesoService` valida empresa/sucursal en cada operación de negocio (403 si es ajena).

---

## Profesionales

Para registrar un profesional se deben crear:

1. El registro en `usuarios`.
2. La pertenencia en `usuarios_empresas`.
3. La asignación del rol `PROFESIONAL`.
4. El registro en `profesionales`.
5. La asignación en `usuarios_sucursales`.

Estas operaciones deben ejecutarse dentro de una única transacción.

## Pacientes

- Un paciente pertenece a una empresa.
- Puede atenderse en distintas sucursales.
- No se relaciona directamente con una sucursal.
- Las sucursales visitadas se obtienen desde las citas.

## Citas

Antes de crear una cita se debe validar que:

- El paciente pertenezca a la empresa.
- La sucursal pertenezca a la empresa.
- El profesional esté activo.
- El profesional pertenezca a la empresa.
- El profesional tenga el rol `PROFESIONAL`.
- El profesional esté asignado a la sucursal.
- El profesional no tenga otra cita en el mismo horario.

## Consultas

- Una cita puede generar como máximo una consulta.
- Una consulta solamente debe iniciarse para una cita confirmada.
- Al finalizar una consulta, la cita debe cambiar a `ATENDIDA`.
- Solo `RECEPCIONISTA` puede ejecutar el cierre operativo de la cita, pero no leer historial clínico ni gestionar recetas.

## Recetas ópticas

- Una consulta puede generar como máximo una receta en el MVP.
- Una receta debe contener un detalle para `OD` y otro para `OI`.
- La adición se almacena una sola vez en `recetas_opticas`.
- El eje debe estar entre 0 y 180.
