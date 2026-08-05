package com.visium.backend.service;

import com.visium.backend.dto.dashboard.DashboardResumenResponse;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.mapper.CitaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.PacienteRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final int CANTIDAD_PROXIMAS_CITAS = 5;

	private final CitaRepository citaRepository;
	private final PacienteRepository pacienteRepository;
	private final CitaMapper citaMapper;
	private final AccesoService accesoService;

	/**
	 * Resumen del dashboard para la empresa activa del usuario y sus sucursales autorizadas.
	 * Todos los datos vienen de la base de datos.
	 */
	@Transactional(readOnly = true)
	public DashboardResumenResponse resumen() {
		UUID empresaId = accesoService.resolverEmpresaObjetivo(null);
		List<UUID> sucursalesVisibles = accesoService.sucursalIdsVisiblesEnEmpresa();

		long totalPacientes = pacienteRepository.countByEmpresaId(empresaId);

		LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
		Instant inicioDelDia = hoy.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant finDelDia = inicioDelDia.plusSeconds(86400);

		long citasConfirmadasHoy =
				citaRepository.contarEnRango(
						empresaId,
						sucursalesVisibles,
						EstadoCita.CONFIRMADA,
						inicioDelDia,
						finDelDia);

		List<com.visium.backend.dto.cita.CitaResponse> proximasCitas =
				citaRepository
						.proximas(
								empresaId,
								sucursalesVisibles,
								EstadoCita.CONFIRMADA,
								Instant.now(),
								PageRequest.of(0, CANTIDAD_PROXIMAS_CITAS))
						.stream()
						.map(citaMapper::toResponse)
						.toList();

		return DashboardResumenResponse.builder()
				.empresaId(empresaId)
				.totalPacientes(totalPacientes)
				.citasConfirmadasHoy(citasConfirmadasHoy)
				.proximasCitas(proximasCitas)
				.build();
	}
}
