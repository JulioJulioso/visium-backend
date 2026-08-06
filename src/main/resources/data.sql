-- Datos iniciales de roles VISIUM.
-- Se ejecuta al arrancar (ver application.yaml: spring.sql.init).

INSERT INTO roles (codigo, nombre)
VALUES
    ('SUPER_ADMIN', 'Dueño Visium (plataforma)'),
    ('JEFE', 'Jefe / Dueño de optica'),
    ('ADMINISTRADOR_SUCURSALES', 'Administrador de sucursales asignadas'),
    ('JEFE_SUCURSAL', 'Jefe de sucursal'),
    ('RECEPCIONISTA', 'Recepcionista')
ON CONFLICT (codigo) DO NOTHING;

-- Migración de esquema para instalaciones previas a numero_registro.
-- Hibernate `update` no siempre agrega esta columna cuando existen restricciones
-- históricas, por lo que se asegura explícitamente antes de consultar perfiles.
ALTER TABLE profesionales
    ADD COLUMN IF NOT EXISTS numero_registro VARCHAR(50);

UPDATE profesionales
SET numero_registro = 'LEGACY-' || id::text
WHERE numero_registro IS NULL OR BTRIM(numero_registro) = '';

ALTER TABLE profesionales
    ALTER COLUMN numero_registro SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_profesionales_numero_registro
    ON profesionales (numero_registro);

-- Migración de perfiles profesionales antiguos: ya no son cuentas de acceso.
ALTER TABLE profesionales
    ALTER COLUMN usuario_id DROP NOT NULL;

-- Desvincula los perfiles del usuario técnico y elimina las cuentas que no
-- tienen citas creadas. Las cuentas con trazabilidad de citas se conservan
-- sin rol para no alterar el historial clínico.
DELETE FROM usuarios_sucursales us
USING usuarios_empresas ue, profesionales p
WHERE us.usuario_empresa_id = ue.id
  AND p.usuario_id = ue.usuario_id
  AND NOT EXISTS (
      SELECT 1 FROM citas c WHERE c.creada_por_usuario_empresa_id = ue.id
  );

DELETE FROM usuarios_empresas ue
USING profesionales p
WHERE p.usuario_id = ue.usuario_id
  AND NOT EXISTS (
      SELECT 1 FROM citas c WHERE c.creada_por_usuario_empresa_id = ue.id
  );

UPDATE profesionales SET usuario_id = NULL WHERE usuario_id IS NOT NULL;



-- Limpieza definitiva de registros heredados que no son sucursales válidas.
-- Se eliminan primero las asignaciones de usuarios para respetar las FK.
DELETE FROM usuarios_sucursales
WHERE sucursal_id IN (
    SELECT id FROM sucursales
    WHERE nombre IS NULL OR BTRIM(nombre) = ''
       OR direccion IS NULL OR BTRIM(direccion) = ''
);

DELETE FROM sucursales
WHERE nombre IS NULL OR BTRIM(nombre) = ''
   OR direccion IS NULL OR BTRIM(direccion) = '';

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

-- La cuenta técnica de plataforma no representa al jefe de una óptica.
-- Conserva exclusivamente SUPER_ADMIN, incluso en instalaciones existentes.
DELETE FROM usuarios_empresas_roles uer
USING usuarios_empresas ue, usuarios u, roles r
WHERE uer.usuario_empresa_id = ue.id
  AND ue.usuario_id = u.id
  AND uer.rol_id = r.id
  AND LOWER(u.email) = 'super@visium.cl'
  AND r.codigo = 'JEFE';

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
DELETE FROM usuarios_empresas_roles WHERE rol_id = (SELECT id FROM roles WHERE codigo = 'PROFESIONAL');
DELETE FROM roles WHERE codigo = 'PROFESIONAL';
