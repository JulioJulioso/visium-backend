-- Seed demo para probar roles (paso 5).
-- Password de todos los usuarios nuevos: admin123
-- Requiere empresas "Optica Visium Demo SpA" y "Optica Norte SpA" (o las crea abajo).
-- Ejecutar: docker exec -i visium-postgres psql -U postgres -d visium < scripts/seed-perfiles-demo.sql

-- Hash BCrypt de "admin123"
-- $2a$10$v/LK2K7sGkcQFhPs34376Oez.W0UzX0HbBX80w2lfsk2L.nG2A28G

INSERT INTO empresas (id, rut, razon_social, email, activo)
SELECT gen_random_uuid(), '76111111-1', 'Optica Visium Demo SpA', 'demo@visium.cl', TRUE
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE razon_social = 'Optica Visium Demo SpA');

INSERT INTO empresas (id, rut, razon_social, email, activo)
SELECT gen_random_uuid(), '76222222-2', 'Optica Norte SpA', 'norte@visium.cl', TRUE
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE razon_social = 'Optica Norte SpA');

INSERT INTO sucursales (empresa_id, nombre, direccion, ciudad, activo)
SELECT e.id, 'Casa Matriz', 'Calle Centro 1', 'Santiago', TRUE
FROM empresas e
WHERE e.razon_social = 'Optica Visium Demo SpA'
  AND NOT EXISTS (
      SELECT 1 FROM sucursales s
      WHERE s.empresa_id = e.id AND s.nombre = 'Casa Matriz'
  );

INSERT INTO sucursales (empresa_id, nombre, direccion, ciudad, activo)
SELECT e.id, 'Sucursal Sur', 'Av. Sur 100', 'Santiago', TRUE
FROM empresas e
WHERE e.razon_social = 'Optica Visium Demo SpA'
  AND NOT EXISTS (
      SELECT 1 FROM sucursales s
      WHERE s.empresa_id = e.id AND s.nombre = 'Sucursal Sur'
  );

INSERT INTO sucursales (empresa_id, nombre, direccion, ciudad, activo)
SELECT e.id, 'Sucursal Norte', 'Av. Norte 200', 'Antofagasta', TRUE
FROM empresas e
WHERE e.razon_social = 'Optica Norte SpA'
  AND NOT EXISTS (
      SELECT 1 FROM sucursales s
      WHERE s.empresa_id = e.id AND s.nombre = 'Sucursal Norte'
  );

-- SUPER_ADMIN (correo de administracion de la plataforma)
INSERT INTO usuarios (id, nombre, apellido, email, password_hash, activo)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'Cristian', 'Fritz', 'cfritzsepulveda8@gmail.com',
    '$2a$10$v/LK2K7sGkcQFhPs34376Oez.W0UzX0HbBX80w2lfsk2L.nG2A28G',
    TRUE
)
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    apellido = EXCLUDED.apellido,
    email = EXCLUDED.email,
    activo = EXCLUDED.activo;

INSERT INTO usuarios_empresas (id, usuario_id, empresa_id, activo)
SELECT
    '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    u.id, e.id, TRUE
FROM usuarios u
CROSS JOIN empresas e
WHERE u.email = 'cfritzsepulveda8@gmail.com'
  AND e.razon_social = 'Optica Visium Demo SpA'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas ue
      WHERE ue.usuario_id = u.id AND ue.empresa_id = e.id
  );

INSERT INTO usuarios_empresas_roles (usuario_empresa_id, rol_id)
SELECT ue.id, r.id
FROM usuarios_empresas ue
JOIN usuarios u ON u.id = ue.usuario_id
JOIN roles r ON r.codigo = 'SUPER_ADMIN'
WHERE u.email = 'cfritzsepulveda8@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas_roles x
      WHERE x.usuario_empresa_id = ue.id AND x.rol_id = r.id
  );

-- JEFE multi-empresa
INSERT INTO usuarios (id, nombre, apellido, email, password_hash, activo)
SELECT
    '22222222-2222-2222-2222-222222222222'::uuid,
    'Jefe', 'Multi', 'jefe@visium.cl',
    '$2a$10$v/LK2K7sGkcQFhPs34376Oez.W0UzX0HbBX80w2lfsk2L.nG2A28G',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'jefe@visium.cl');

INSERT INTO usuarios_empresas (usuario_id, empresa_id, activo)
SELECT u.id, e.id, TRUE
FROM usuarios u
CROSS JOIN empresas e
WHERE u.email = 'jefe@visium.cl'
  AND e.razon_social IN ('Optica Visium Demo SpA', 'Optica Norte SpA')
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas ue
      WHERE ue.usuario_id = u.id AND ue.empresa_id = e.id
  );

INSERT INTO usuarios_empresas_roles (usuario_empresa_id, rol_id)
SELECT ue.id, r.id
FROM usuarios_empresas ue
JOIN usuarios u ON u.id = ue.usuario_id
JOIN roles r ON r.codigo = 'JEFE'
WHERE u.email = 'jefe@visium.cl'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas_roles x
      WHERE x.usuario_empresa_id = ue.id AND x.rol_id = r.id
  );

-- JEFE_SUCURSAL solo Casa Matriz
INSERT INTO usuarios (id, nombre, apellido, email, password_hash, activo)
SELECT
    '33333333-3333-3333-3333-333333333333'::uuid,
    'Jefe', 'Sucursal', 'jsucursal@visium.cl',
    '$2a$10$v/LK2K7sGkcQFhPs34376Oez.W0UzX0HbBX80w2lfsk2L.nG2A28G',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'jsucursal@visium.cl');

INSERT INTO usuarios_empresas (id, usuario_id, empresa_id, activo)
SELECT
    '33333333-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    u.id, e.id, TRUE
FROM usuarios u
CROSS JOIN empresas e
WHERE u.email = 'jsucursal@visium.cl'
  AND e.razon_social = 'Optica Visium Demo SpA'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas ue
      WHERE ue.usuario_id = u.id AND ue.empresa_id = e.id
  );

INSERT INTO usuarios_empresas_roles (usuario_empresa_id, rol_id)
SELECT ue.id, r.id
FROM usuarios_empresas ue
JOIN usuarios u ON u.id = ue.usuario_id
JOIN roles r ON r.codigo = 'JEFE_SUCURSAL'
WHERE u.email = 'jsucursal@visium.cl'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_empresas_roles x
      WHERE x.usuario_empresa_id = ue.id AND x.rol_id = r.id
  );

INSERT INTO usuarios_sucursales (usuario_empresa_id, sucursal_id, empresa_id)
SELECT ue.id, s.id, s.empresa_id
FROM usuarios_empresas ue
JOIN usuarios u ON u.id = ue.usuario_id
JOIN sucursales s ON s.empresa_id = ue.empresa_id AND s.nombre = 'Casa Matriz'
WHERE u.email = 'jsucursal@visium.cl'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios_sucursales us
      WHERE us.usuario_empresa_id = ue.id AND us.sucursal_id = s.id
  );
