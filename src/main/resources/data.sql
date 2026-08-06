-- Datos iniciales de roles VISIUM.
-- Se ejecuta al arrancar (ver application.yaml: spring.sql.init).

INSERT INTO roles (codigo, nombre)
VALUES
    ('SUPER_ADMIN', 'Dueño Visium (plataforma)'),
    ('JEFE', 'Jefe / Dueño de optica'),
    ('ADMINISTRADOR_SUCURSALES', 'Administrador de sucursales asignadas'),
    ('JEFE_SUCURSAL', 'Jefe de sucursal'),
    ('RECEPCIONISTA', 'Recepcionista'),
    ('PROFESIONAL', 'Profesional')
ON CONFLICT (codigo) DO NOTHING;

-- Migracion one-shot: asignaciones legacy ADMIN -> JEFE, luego elimina el rol ADMIN.
UPDATE usuarios_empresas_roles uer
SET rol_id = (SELECT id FROM roles WHERE codigo = 'JEFE')
WHERE rol_id = (SELECT id FROM roles WHERE codigo = 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM usuarios_empresas_roles x
      WHERE x.usuario_empresa_id = uer.usuario_empresa_id
        AND x.rol_id = (SELECT id FROM roles WHERE codigo = 'JEFE')
  );

DELETE FROM usuarios_empresas_roles
WHERE rol_id = (SELECT id FROM roles WHERE codigo = 'ADMIN');

DELETE FROM roles WHERE codigo = 'ADMIN';

-- Cuenta de demostracion de plataforma: conserva SUPER_ADMIN y tambien recibe
-- JEFE en cada empresa a la que ya pertenece. Es idempotente y no crea cuentas.
INSERT INTO usuarios_empresas_roles (usuario_empresa_id, rol_id)
SELECT ue.id, r.id
FROM usuarios_empresas ue
JOIN usuarios u ON u.id = ue.usuario_id
JOIN roles r ON r.codigo = 'JEFE'
WHERE LOWER(u.email) = 'super@visium.cl'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas_roles asignacion
      WHERE asignacion.usuario_empresa_id = ue.id AND asignacion.rol_id = r.id
  );

-- Cuenta técnica: si existe, debe tener alcance efectivo sobre las empresas.
-- Evita JWT sin roles/empresas al iniciar sesión como super@visium.cl.
INSERT INTO usuarios_empresas (usuario_id, empresa_id, activo)
SELECT u.id, e.id, TRUE
FROM usuarios u CROSS JOIN empresas e
WHERE LOWER(u.email) = 'super@visium.cl'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas ue
      WHERE ue.usuario_id = u.id AND ue.empresa_id = e.id
  );

INSERT INTO usuarios_empresas_roles (usuario_empresa_id, rol_id)
SELECT ue.id, r.id
FROM usuarios_empresas ue
JOIN usuarios u ON u.id = ue.usuario_id
JOIN roles r ON r.codigo = 'SUPER_ADMIN'
WHERE LOWER(u.email) = 'super@visium.cl'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas_roles asignacion
      WHERE asignacion.usuario_empresa_id = ue.id AND asignacion.rol_id = r.id
  );
