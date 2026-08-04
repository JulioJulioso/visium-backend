# Colección Bruno — Visium Backend

Abrir esta carpeta en [Bruno](https://www.usebruno.com/).

## Usuarios demo (password: `admin123`)

| Email | Rol | Alcance |
|---|---|---|
| `cfritzsepulveda8@gmail.com` | SUPER_ADMIN | Todas las empresas |
| `jefe@visium.cl` | JEFE | Demo + Norte |
| `jsucursal@visium.cl` | JEFE_SUCURSAL | Solo Casa Matriz (Demo) |

## Seed

Con Docker Postgres arriba:

```powershell
Get-Content .\scripts\seed-perfiles-demo.sql | docker exec -i visium-postgres psql -U postgres -d visium
```

## Orden de prueba

1. Environment **Local** (`baseUrl=http://localhost:8080`)
2. Login SUPER_ADMIN → Listar empresas (debe ver Demo y Norte)
3. Login JEFE → Listar empresas (2) → Sucursales Demo → Sucursales Norte con `X-Empresa-Id`
4. Login JEFE_SUCURSAL → Sucursales Demo (solo Casa Matriz) → Sucursales Norte (403)
