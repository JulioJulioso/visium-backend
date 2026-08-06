-- Datos de prueba para Óptica Visium Demo Span / SpA, sucursal Casa Matriz.
-- Es idempotente: puede ejecutarse más de una vez sin duplicar registros.
-- Ejecutar: docker exec -i visium-postgres psql -U postgres -d visium < scripts/seed-datos-prueba-demo.sql

DO $$
DECLARE
    v_empresa_id UUID;
    v_sucursal_id UUID;
    indice INTEGER;
BEGIN
    -- "Span" es el nombre solicitado; se admite el nombre histórico "SpA".
    SELECT id INTO v_empresa_id
    FROM empresas
    WHERE razon_social IN ('Optica Visium Demo Span', 'Optica Visium Demo SpA')
    ORDER BY CASE razon_social WHEN 'Optica Visium Demo Span' THEN 0 ELSE 1 END
    LIMIT 1;

    IF v_empresa_id IS NULL THEN
        RAISE EXCEPTION 'No se encontró la empresa Óptica Visium Demo Span / SpA';
    END IF;

    SELECT id INTO v_sucursal_id
    FROM sucursales
    WHERE empresa_id = v_empresa_id AND nombre = 'Casa Matriz'
    LIMIT 1;

    IF v_sucursal_id IS NULL THEN
        RAISE EXCEPTION 'No se encontró la sucursal Casa Matriz para la empresa de demostración';
    END IF;

    FOR indice IN 1..30 LOOP
        INSERT INTO pacientes (
            id, empresa_id, tipo_documento, numero_documento, nombre, apellido,
            fecha_nacimiento, sexo, telefono, email, direccion, activo, created_at, updated_at
        )
        SELECT
            gen_random_uuid(), v_empresa_id, 'RUN',
            '12.' || lpad(indice::TEXT, 3, '0') || '.123-' || (indice % 10),
            'Paciente', 'Prueba ' || lpad(indice::TEXT, 2, '0'),
            DATE '1980-01-01' + (indice * 173),
            CASE WHEN indice % 2 = 0 THEN 'FEMENINO' ELSE 'MASCULINO' END,
            '+5697000' || lpad(indice::TEXT, 4, '0'),
            'paciente.prueba.' || lpad(indice::TEXT, 2, '0') || '@visium-demo.cl',
            'Dirección de prueba ' || indice || ', Santiago', TRUE, NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM pacientes
            WHERE email = 'paciente.prueba.' || lpad(indice::TEXT, 2, '0') || '@visium-demo.cl'
        );

        INSERT INTO profesionales (
            id, empresa_id, sucursal_id, nombre, apellido, email, run, telefono,
            numero_registro, especialidad, activo, created_at, updated_at
        )
        SELECT
            gen_random_uuid(), v_empresa_id, v_sucursal_id,
            'Profesional', 'Prueba ' || lpad(indice::TEXT, 2, '0'),
            'profesional.prueba.' || lpad(indice::TEXT, 2, '0') || '@visium-demo.cl',
            '15.' || lpad(indice::TEXT, 3, '0') || '.456-' || (indice % 10),
            '+5698000' || lpad(indice::TEXT, 4, '0'),
            'DEMO-PROF-' || lpad(indice::TEXT, 3, '0'),
            CASE indice % 5
                WHEN 0 THEN 'Oftalmología'
                WHEN 1 THEN 'Optometría'
                WHEN 2 THEN 'Contactología'
                WHEN 3 THEN 'Baja visión'
                ELSE 'Oftalmología pediátrica'
            END,
            TRUE, NOW(), NOW()
        WHERE NOT EXISTS (
            SELECT 1 FROM profesionales
            WHERE numero_registro = 'DEMO-PROF-' || lpad(indice::TEXT, 3, '0')
        );
    END LOOP;
END $$;
