-- Diez citas de demostración para la empresa Óptica Visium Demo Span / SpA,
-- sucursal Casa Matriz. Los horarios son bloques de 30 minutos, igual que el
-- formulario de agendamiento (09:00, 09:30, ... 13:30), en hora de Santiago.
-- El script es idempotente: volver a ejecutarlo no duplica las citas.

DO $$
DECLARE
    v_empresa_id UUID;
    v_sucursal_id UUID;
    v_usuario_empresa_id UUID;
BEGIN
    SELECT id INTO v_empresa_id
    FROM empresas
    WHERE razon_social IN ('Optica Visium Demo Span', 'Optica Visium Demo SpA')
    ORDER BY CASE razon_social WHEN 'Optica Visium Demo Span' THEN 0 ELSE 1 END
    LIMIT 1;

    SELECT id INTO v_sucursal_id
    FROM sucursales
    WHERE empresa_id = v_empresa_id AND nombre = 'Casa Matriz'
    LIMIT 1;

    SELECT id INTO v_usuario_empresa_id
    FROM usuarios_empresas
    WHERE empresa_id = v_empresa_id AND activo = TRUE
    ORDER BY created_at
    LIMIT 1;

    IF v_empresa_id IS NULL OR v_sucursal_id IS NULL OR v_usuario_empresa_id IS NULL THEN
        RAISE EXCEPTION 'Falta empresa, sucursal Casa Matriz o un usuario activo de la empresa de demostración';
    END IF;

    INSERT INTO citas (
        id, empresa_id, sucursal_id, paciente_id, profesional_id,
        creada_por_usuario_empresa_id, fecha_hora_inicio, fecha_hora_fin,
        estado, motivo, observaciones, created_at, updated_at
    )
    SELECT
        gen_random_uuid(), v_empresa_id, v_sucursal_id, paciente.id, profesional.id,
        v_usuario_empresa_id,
        ((CURRENT_DATE + TIME '09:00' + ((serie.indice - 1) * INTERVAL '30 minutes')) AT TIME ZONE 'America/Santiago'),
        ((CURRENT_DATE + TIME '09:30' + ((serie.indice - 1) * INTERVAL '30 minutes')) AT TIME ZONE 'America/Santiago'),
        'PENDIENTE', 'Consulta visual de prueba', 'Cita de demostración generada automáticamente.', NOW(), NOW()
    FROM generate_series(1, 10) AS serie(indice)
    JOIN LATERAL (
        SELECT id FROM pacientes
        WHERE empresa_id = v_empresa_id AND sucursal_id = v_sucursal_id AND activo = TRUE
        ORDER BY email, id
        OFFSET serie.indice - 1 LIMIT 1
    ) AS paciente ON TRUE
    JOIN LATERAL (
        SELECT id FROM profesionales
        WHERE empresa_id = v_empresa_id AND sucursal_id = v_sucursal_id AND activo = TRUE
        ORDER BY numero_registro, id
        OFFSET serie.indice - 1 LIMIT 1
    ) AS profesional ON TRUE
    WHERE NOT EXISTS (
        SELECT 1 FROM citas existente
        WHERE existente.paciente_id = paciente.id
          AND existente.fecha_hora_inicio = ((CURRENT_DATE + TIME '09:00' + ((serie.indice - 1) * INTERVAL '30 minutes')) AT TIME ZONE 'America/Santiago')
    );
END $$;
