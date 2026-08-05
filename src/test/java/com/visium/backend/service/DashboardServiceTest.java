package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.dashboard.DashboardResumenResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.mapper.CitaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.PacienteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID SUCURSAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Mock private CitaRepository citaRepository;
	@Mock private PacienteRepository pacienteRepository;
	@Mock private CitaMapper citaMapper;
	@Mock private AccesoService accesoService;

	private DashboardService service;

	@BeforeEach
	void setUp() {
		service =
				new DashboardService(
						citaRepository, pacienteRepository, citaMapper, accesoService);
	}

	@Test
	void resumen_devuelveTotalesYProximasCitasDeSucursalesAutorizadas() {
		when(accesoService.resolverEmpresaObjetivo(null)).thenReturn(EMPRESA_ID);
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of(SUCURSAL_ID));
		when(pacienteRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(42L);
		when(citaRepository.contarEnRango(
						eq(EMPRESA_ID),
						eq(List.of(SUCURSAL_ID)),
						eq(EstadoCita.CONFIRMADA),
						any(Instant.class),
						any(Instant.class)))
				.thenReturn(3L);

		Cita cita = new Cita();
		cita.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
		when(citaRepository.proximas(
						eq(EMPRESA_ID),
						eq(List.of(SUCURSAL_ID)),
						eq(EstadoCita.CONFIRMADA),
						any(Instant.class),
						any(Pageable.class)))
				.thenReturn(List.of(cita));
		when(citaMapper.toResponse(cita)).thenReturn(null);

		DashboardResumenResponse resumen = service.resumen();

		assertEquals(EMPRESA_ID, resumen.getEmpresaId());
		assertEquals(42L, resumen.getTotalPacientes());
		assertEquals(3L, resumen.getCitasConfirmadasHoy());
		assertEquals(1, resumen.getProximasCitas().size());
	}

	@Test
	void resumen_sinSucursalesCuentaTodaLaEmpresa() {
		when(accesoService.resolverEmpresaObjetivo(null)).thenReturn(EMPRESA_ID);
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of());
		when(pacienteRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(7L);
		when(citaRepository.contarEnRango(
						eq(EMPRESA_ID),
						eq(List.of()),
						eq(EstadoCita.CONFIRMADA),
						any(Instant.class),
						any(Instant.class)))
				.thenReturn(1L);
		when(citaRepository.proximas(
						eq(EMPRESA_ID),
						eq(List.of()),
						eq(EstadoCita.CONFIRMADA),
						any(Instant.class),
						any(Pageable.class)))
				.thenReturn(List.of());

		DashboardResumenResponse resumen = service.resumen();

		assertEquals(0, resumen.getProximasCitas().size());
	}

	@Test
	void resumen_lanzaSiNoHayEmpresaResuelta() {
		when(accesoService.resolverEmpresaObjetivo(null))
				.thenThrow(new ForbiddenException("No tienes empresas asignadas"));

		assertThrows(ForbiddenException.class, () -> service.resumen());
		verify(pacienteRepository, never()).countByEmpresaId(any());
	}
}
