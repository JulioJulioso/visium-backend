package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.cita.CitaRequest;
import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Consulta;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.entity.Usuario;
import com.visium.backend.entity.UsuarioEmpresa;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import org.mockito.ArgumentCaptor;
import com.visium.backend.mapper.CitaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.PacienteRepository;
import com.visium.backend.repository.ProfesionalRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.UsuarioEmpresaRepository;
import com.visium.backend.security.UsuarioDetails;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

	private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID OTRA_EMPRESA_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID SUCURSAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTRA_SUCURSAL_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
	private static final UUID PROFESIONAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID OTRO_PROFESIONAL_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
	private static final UUID USUARIO_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
	private static final UUID OTRO_USUARIO_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
	private static final UUID PACIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID OTRO_PACIENTE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	private static final UUID CITA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	@Mock
	private CitaRepository citaRepository;

	@Mock
	private ProfesionalRepository profesionalRepository;

	@Mock
	private SucursalRepository sucursalRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private ConsultaRepository consultaRepository;

	@Mock
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Mock
	private CitaMapper citaMapper;

	@Mock
	private AccesoService accesoService;

	private CitaService citaService;

	@BeforeEach
	void setUp() {
		citaService = new CitaService(
				citaRepository,
				profesionalRepository,
				sucursalRepository,
				pacienteRepository,
				consultaRepository,
				usuarioEmpresaRepository,
				citaMapper,
				accesoService);
	}

	@Test
	void listaCitasConfirmadasDeUnProfesional() {
		UsuarioDetails detalles = autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.usuarioActual()).thenReturn(detalles);
		when(accesoService.esSuperAdmin()).thenReturn(false);
		when(accesoService.esJefeDeEmpresa()).thenReturn(true);
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional()));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID))
				.thenReturn(List.of(pertenencia()));
		when(citaRepository.findByProfesionalIdAndFechaHoraInicioBetweenAndEstado(
				eq(PROFESIONAL_ID), any(Instant.class), any(Instant.class), eq(EstadoCita.CONFIRMADA)))
				.thenReturn(List.of(cita()));
		when(citaMapper.toResponse(any(Cita.class))).thenReturn(citaResponse());

		List<CitaResponse> citas = citaService.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10));

		assertEquals(1, citas.size());
		assertEquals(PROFESIONAL_ID, citas.getFirst().getProfesionalId());
		assertEquals(EstadoCita.CONFIRMADA, citas.getFirst().getEstado());
		verify(accesoService).exigirAccesoEmpresa(EMPRESA_ID);
	}

	@Test
	void listaCitasConfirmadasSinFiltroDeFechas() {
		UsuarioDetails detalles = autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.usuarioActual()).thenReturn(detalles);
		when(accesoService.esSuperAdmin()).thenReturn(false);
		when(accesoService.esJefeDeEmpresa()).thenReturn(true);
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional()));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID))
				.thenReturn(List.of(pertenencia()));
		when(citaRepository.findByProfesionalIdAndEstadoOrderByFechaHoraInicioAsc(
				PROFESIONAL_ID, EstadoCita.CONFIRMADA))
				.thenReturn(List.of(cita()));
		when(citaMapper.toResponse(any(Cita.class))).thenReturn(citaResponse());

		List<CitaResponse> citas = citaService.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, null, null);

		assertEquals(1, citas.size());
		verify(citaRepository).findByProfesionalIdAndEstadoOrderByFechaHoraInicioAsc(
				PROFESIONAL_ID, EstadoCita.CONFIRMADA);
	}

	@Test
	void rangoInvalidoFalla() {
		assertThrows(BadRequestException.class, () -> citaService.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 3)));

		verify(citaRepository, never()).findByProfesionalIdAndFechaHoraInicioBetweenAndEstado(
				any(), any(), any(), any());
	}

	@Test
	void profesionalNoPuedeVerCitasDeOtroProfesional() {
		UsuarioDetails detalles = autenticar(roles("PROFESIONAL"), List.of(EMPRESA_ID), List.of(SUCURSAL_ID));
		when(accesoService.usuarioActual()).thenReturn(detalles);
		when(accesoService.esSuperAdmin()).thenReturn(false);
		when(accesoService.esJefeDeEmpresa()).thenReturn(false);
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional()));

		assertThrows(ForbiddenException.class, () -> citaService.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10)));

		verify(citaRepository, never()).findByProfesionalIdAndFechaHoraInicioBetweenAndEstado(
				any(), any(), any(), any());
	}

	// ===== listarCitas (listado general por empresa) =====

	@Test
	void listarCitasDeEmpresaConFiltros() {
		autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.resolverEmpresaObjetivo(null)).thenReturn(EMPRESA_ID);
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of());
		when(citaRepository.listarEnRango(
				eq(EMPRESA_ID), eq(List.of()), eq(EstadoCita.CONFIRMADA),
				any(Instant.class), any(Instant.class)))
				.thenReturn(List.of(cita()));
		when(citaMapper.toResponse(any(Cita.class))).thenReturn(citaResponse());

		List<CitaResponse> citas = citaService.listarCitas(
				EstadoCita.CONFIRMADA, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10));

		assertEquals(1, citas.size());
		assertEquals(EstadoCita.CONFIRMADA, citas.getFirst().getEstado());
		verify(accesoService).resolverEmpresaObjetivo(null);
	}

	@Test
	void listarCitasSinFiltrosUsaRangoCompleto() {
		autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.resolverEmpresaObjetivo(null)).thenReturn(EMPRESA_ID);
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of());
		when(citaRepository.listarEnRango(any(), any(), any(), any(), any())).thenReturn(List.of(cita()));
		when(citaMapper.toResponse(any(Cita.class))).thenReturn(citaResponse());

		List<CitaResponse> citas = citaService.listarCitas(null, null, null);

		assertEquals(1, citas.size());
		verify(citaRepository).listarEnRango(
				eq(EMPRESA_ID), eq(List.of()), isNull(), any(Instant.class), any(Instant.class));
	}

	@Test
	void listarCitasConRangoInvalidoFalla() {
		assertThrows(BadRequestException.class, () -> citaService.listarCitas(
				null, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 3)));

		verify(citaRepository, never()).listarEnRango(any(), any(), any(), any(), any());
	}

	@Test
	void listarCitasFiltraLasDeSucursalesSinAcceso() {
		autenticar(roles("JEFE_SUCURSAL"), List.of(EMPRESA_ID), List.of(OTRA_SUCURSAL_ID));
		when(accesoService.resolverEmpresaObjetivo(null)).thenReturn(EMPRESA_ID);
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of(OTRA_SUCURSAL_ID));
		when(citaRepository.listarEnRango(any(), any(), any(), any(), any())).thenReturn(List.of(cita()));
		doThrow(new ForbiddenException("No tienes acceso a esa sucursal"))
				.when(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);

		List<CitaResponse> citas = citaService.listarCitas(null, null, null);

		assertEquals(0, citas.size());
	}

	// ===== crearCita: casos raros / frontera =====

	@Test
	void crearCitaExitosamente() {
		UsuarioDetails detalles = autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.usuarioActual()).thenReturn(detalles);
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente(EMPRESA_ID)));
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional()));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID))
				.thenReturn(List.of(pertenencia(EMPRESA_ID)));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaResponse respuesta = serviceConMapperReal().crearCita(citaRequest());

		assertEquals(EMPRESA_ID, respuesta.getEmpresaId());
		assertEquals(SUCURSAL_ID, respuesta.getSucursalId());
		assertEquals(EstadoCita.PENDIENTE, respuesta.getEstado());
		verify(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);
	}

	@Test
	void crearCitaConSucursalInexistenteLanzaNotFound() {
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConPacienteInexistenteLanzaNotFound() {
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConProfesionalInexistenteLanzaNotFound() {
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente(EMPRESA_ID)));
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaCuandoUsuarioNoPerteneceALaEmpresaLanzaForbidden() {
		UsuarioDetails detalles = autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.usuarioActual()).thenReturn(detalles);
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente(EMPRESA_ID)));
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional(OTRO_USUARIO_ID)));
		when(usuarioEmpresaRepository.findByUsuarioId(OTRO_USUARIO_ID))
				.thenReturn(List.of(pertenencia(EMPRESA_ID)));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of());

		assertThrows(ForbiddenException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConEstadoNullSeGuardaPendiente() {
		UsuarioDetails detalles = autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());
		when(accesoService.usuarioActual()).thenReturn(detalles);
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente(EMPRESA_ID)));
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional()));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID))
				.thenReturn(List.of(pertenencia(EMPRESA_ID)));

		serviceConMapperReal().crearCita(citaRequest());

		ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
		verify(citaRepository).save(captor.capture());
		assertNotNull(captor.getValue().getCreadaPor());
		assertEquals(EstadoCita.PENDIENTE, captor.getValue().getEstado());
	}

	@Test
	void crearCitaConSucursalDeOtraEmpresaRechazada() {
		// FIX: se valida que la sucursal pertenezca a la empresa de la cita.
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, OTRA_EMPRESA_ID)));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConPacienteDeOtraEmpresaRechazada() {
		// FIX: se valida que el paciente pertenezca a la empresa de la cita.
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente(OTRA_EMPRESA_ID)));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConProfesionalDeOtraEmpresaRechazada() {
		// FIX: se valida que el profesional pertenezca a la empresa de la cita.
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal(SUCURSAL_ID, EMPRESA_ID)));
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente(EMPRESA_ID)));
		when(profesionalRepository.findById(PROFESIONAL_ID)).thenReturn(Optional.of(profesional(OTRO_USUARIO_ID)));
		when(usuarioEmpresaRepository.findByUsuarioId(OTRO_USUARIO_ID))
				.thenReturn(List.of(pertenencia(OTRA_EMPRESA_ID)));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConFinAntesDeInicioRechazada() {
		// FIX: se valida que fin sea posterior a inicio.
		CitaRequest req = citaRequest();
		req.setFechaHoraFin(Instant.parse("2026-08-10T08:00:00Z"));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConFechasIgualesRechazada() {
		CitaRequest req = citaRequest();
		req.setFechaHoraFin(req.getFechaHoraInicio());

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConEstadoConfirmadoRechazada() {
		// FIX: una cita nueva solo puede crearse PENDIENTE.
		CitaRequest req = citaRequest();
		req.setEstado(EstadoCita.CONFIRMADA);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void crearCitaConPacienteIdNullLanzaBadRequest() {
		// FIX: el service valida los campos obligatorios aunque no haya @Valid.
		CitaRequest req = citaRequest();
		req.setPacienteId(null);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().crearCita(req));

		verify(citaRepository, never()).save(any());
	}

	// ===== modificarCita: casos raros / frontera =====

	@Test
	void modificarCitaInexistenteLanzaNotFound() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaExitosamente() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaRequest req = citaRequest();
		req.setMotivo("Revisión reprogramada");

		CitaResponse respuesta = serviceConMapperReal().modificarCita(CITA_ID, req);

		assertEquals("Revisión reprogramada", respuesta.getMotivo());
		assertEquals(EstadoCita.CONFIRMADA, respuesta.getEstado());
		verify(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);
	}

	@Test
	void modificarCitaSinAccesoALaSucursalActualLanzaForbidden() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		doThrow(new ForbiddenException("No tienes acceso a esa sucursal"))
				.when(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);

		assertThrows(ForbiddenException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaCambiandoEmpresaAjenaLanzaForbidden() {
		// FIX: se valida acceso a la empresa nueva aunque la sucursal no cambie.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		doThrow(new ForbiddenException("No tienes acceso a esa empresa"))
				.when(accesoService).exigirAccesoEmpresa(OTRA_EMPRESA_ID);

		CitaRequest req = citaRequest();
		req.setEmpresaId(OTRA_EMPRESA_ID);

		assertThrows(ForbiddenException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaCambiandoSoloEmpresaIdConSucursalInconsistenteRechazada() {
		// FIX: cambiar la empresa sin cambiar la sucursal deja la cita inconsistente → 400.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));

		CitaRequest req = citaRequest();
		req.setEmpresaId(OTRA_EMPRESA_ID);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaCambiandoEmpresaYSucursalCoherentesPermitida() {
		// FIX: mover la cita completa a otra empresa (empresa + sucursal de la misma) sí es válido.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(sucursalRepository.findById(OTRA_SUCURSAL_ID)).thenReturn(Optional.of(sucursal(OTRA_SUCURSAL_ID, OTRA_EMPRESA_ID)));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaRequest req = citaRequest();
		req.setEmpresaId(OTRA_EMPRESA_ID);
		req.setSucursalId(OTRA_SUCURSAL_ID);

		CitaResponse respuesta = serviceConMapperReal().modificarCita(CITA_ID, req);

		assertEquals(OTRA_EMPRESA_ID, respuesta.getEmpresaId());
		assertEquals(OTRA_SUCURSAL_ID, respuesta.getSucursalId());
		verify(accesoService).exigirAccesoEmpresa(OTRA_EMPRESA_ID);
		verify(accesoService).exigirAccesoSucursal(OTRA_EMPRESA_ID, OTRA_SUCURSAL_ID);
	}

	@Test
	void modificarCitaCambiandoSucursalDeOtraEmpresaRechazada() {
		// FIX: la sucursal nueva debe pertenecer a la empresa de la cita.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(sucursalRepository.findById(OTRA_SUCURSAL_ID)).thenReturn(Optional.of(sucursal(OTRA_SUCURSAL_ID, OTRA_EMPRESA_ID)));

		CitaRequest req = citaRequest();
		req.setSucursalId(OTRA_SUCURSAL_ID);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaCambiandoPacienteDeOtraEmpresaRechazada() {
		// FIX: el paciente nuevo debe pertenecer a la empresa de la cita.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(pacienteRepository.findById(OTRO_PACIENTE_ID)).thenReturn(Optional.of(paciente(OTRO_PACIENTE_ID, OTRA_EMPRESA_ID)));

		CitaRequest req = citaRequest();
		req.setPacienteId(OTRO_PACIENTE_ID);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaCambiandoProfesionalDeOtraEmpresaRechazada() {
		// FIX: el profesional nuevo debe pertenecer a la empresa de la cita.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(profesionalRepository.findById(OTRO_PROFESIONAL_ID)).thenReturn(Optional.of(profesional(OTRO_USUARIO_ID)));
		when(usuarioEmpresaRepository.findByUsuarioId(OTRO_USUARIO_ID))
				.thenReturn(List.of(pertenencia(OTRA_EMPRESA_ID)));

		CitaRequest req = citaRequest();
		req.setProfesionalId(OTRO_PROFESIONAL_ID);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaConFinAntesDeInicioRechazada() {
		// FIX: se valida que fin sea posterior a inicio con los valores finales.
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));

		CitaRequest req = citaRequest();
		req.setFechaHoraInicio(Instant.parse("2026-08-10T10:00:00Z"));
		req.setFechaHoraFin(Instant.parse("2026-08-10T09:00:00Z"));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaConEstadoNullNoCambiaEstado() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaResponse respuesta = serviceConMapperReal().modificarCita(CITA_ID, citaRequest());

		assertEquals(EstadoCita.CONFIRMADA, respuesta.getEstado());
	}

	@Test
	void modificarCitaConTransicionPendienteAConfirmadaPermitida() {
		Cita existente = cita();
		existente.setEstado(EstadoCita.PENDIENTE);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(existente));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaRequest req = citaRequest();
		req.setEstado(EstadoCita.CONFIRMADA);

		CitaResponse respuesta = serviceConMapperReal().modificarCita(CITA_ID, req);

		assertEquals(EstadoCita.CONFIRMADA, respuesta.getEstado());
	}

	@Test
	void modificarCitaConTransicionConfirmadaACanceladaPermitida() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaRequest req = citaRequest();
		req.setEstado(EstadoCita.CANCELADA);

		CitaResponse respuesta = serviceConMapperReal().modificarCita(CITA_ID, req);

		assertEquals(EstadoCita.CANCELADA, respuesta.getEstado());
	}

	@Test
	void modificarCitaCanceladaAPendientePermitida() {
		// FIX: permite reagendar sobre una cita cancelada (flujo del frontend).
		Cita existente = cita();
		existente.setEstado(EstadoCita.CANCELADA);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(existente));
		when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		CitaRequest req = citaRequest();
		req.setEstado(EstadoCita.PENDIENTE);

		CitaResponse respuesta = serviceConMapperReal().modificarCita(CITA_ID, req);

		assertEquals(EstadoCita.PENDIENTE, respuesta.getEstado());
	}

	@Test
	void modificarCitaConTransicionInvalidaRechazada() {
		// FIX: no se puede saltar de PENDIENTE a NO_ASISTIO.
		Cita existente = cita();
		existente.setEstado(EstadoCita.PENDIENTE);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(existente));

		CitaRequest req = citaRequest();
		req.setEstado(EstadoCita.NO_ASISTIO);

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, req));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaAtendidaRechazada() {
		// FIX: ATENDIDA es un estado terminal; la cita no se puede modificar.
		Cita existente = cita();
		existente.setEstado(EstadoCita.ATENDIDA);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(existente));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	@Test
	void modificarCitaNoAsistioRechazada() {
		Cita existente = cita();
		existente.setEstado(EstadoCita.NO_ASISTIO);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(existente));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().modificarCita(CITA_ID, citaRequest()));

		verify(citaRepository, never()).save(any());
	}

	// ===== eliminarCita: casos raros / frontera =====

	@Test
	void eliminarCitaInexistenteLanzaNotFound() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> serviceConMapperReal().eliminarCita(CITA_ID));

		verify(citaRepository, never()).delete(any());
	}

	@Test
	void eliminarCitaSinAccesoALaSucursalLanzaForbidden() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		doThrow(new ForbiddenException("No tienes acceso a esa sucursal"))
				.when(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);

		assertThrows(ForbiddenException.class, () -> serviceConMapperReal().eliminarCita(CITA_ID));

		verify(citaRepository, never()).delete(any());
	}

	@Test
	void eliminarCitaExitosamente() {
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));

		serviceConMapperReal().eliminarCita(CITA_ID);

		verify(citaRepository).delete(any(Cita.class));
	}

	@Test
	void eliminarCitaConConsultaAsociadaRechazada() {
		// FIX: no se borra físicamente una cita que ya tiene consulta (protege la FK).
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita()));
		when(consultaRepository.findByCitaId(CITA_ID)).thenReturn(Optional.of(new Consulta()));

		assertThrows(BadRequestException.class, () -> serviceConMapperReal().eliminarCita(CITA_ID));

		verify(citaRepository, never()).delete(any());
	}

	@Test
	void eliminarCitaSinConsultaSePermiteAunAtendida() {
		// La protección de integridad está en la FK: sin consulta, se elimina.
		Cita existente = cita();
		existente.setEstado(EstadoCita.ATENDIDA);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(existente));
		when(consultaRepository.findByCitaId(CITA_ID)).thenReturn(Optional.empty());

		serviceConMapperReal().eliminarCita(CITA_ID);

		verify(citaRepository).delete(existente);
	}

	private CitaRequest citaRequest() {
		return CitaRequest.builder()
				.empresaId(EMPRESA_ID)
				.sucursalId(SUCURSAL_ID)
				.pacienteId(PACIENTE_ID)
				.profesionalId(PROFESIONAL_ID)
				.fechaHoraInicio(Instant.parse("2026-08-10T09:00:00Z"))
				.fechaHoraFin(Instant.parse("2026-08-10T09:30:00Z"))
				.build();
	}

	private CitaService serviceConMapperReal() {
		return new CitaService(
				citaRepository,
				profesionalRepository,
				sucursalRepository,
				pacienteRepository,
				consultaRepository,
				usuarioEmpresaRepository,
				new CitaMapper(),
				accesoService);
	}

	private Sucursal sucursal(UUID id, UUID empresaId) {
		Empresa empresa = new Empresa();
		empresa.setId(empresaId);
		Sucursal sucursal = new Sucursal();
		sucursal.setId(id);
		sucursal.setEmpresa(empresa);
		return sucursal;
	}

	private Paciente paciente(UUID empresaId) {
		return paciente(PACIENTE_ID, empresaId);
	}

	private Paciente paciente(UUID id, UUID empresaId) {
		Empresa empresa = new Empresa();
		empresa.setId(empresaId);
		Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setEmpresa(empresa);
		return paciente;
	}

	private CitaResponse citaResponse() {
		return CitaResponse.builder()
				.id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
				.empresaId(EMPRESA_ID)
				.sucursalId(SUCURSAL_ID)
				.pacienteId(PACIENTE_ID)
				.profesionalId(PROFESIONAL_ID)
				.fechaHoraInicio(Instant.parse("2026-08-05T15:00:00Z"))
				.fechaHoraFin(Instant.parse("2026-08-05T15:30:00Z"))
				.estado(EstadoCita.CONFIRMADA)
				.build();
	}

	private Cita cita() {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);

		Sucursal sucursal = new Sucursal();
		sucursal.setId(SUCURSAL_ID);
		sucursal.setEmpresa(empresa);

		Paciente paciente = new Paciente();
		paciente.setId(PACIENTE_ID);
		paciente.setEmpresa(empresa);

		Cita cita = new Cita();
		cita.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
		cita.setEmpresaId(EMPRESA_ID);
		cita.setSucursal(sucursal);
		cita.setPaciente(paciente);
		cita.setProfesional(profesional());
		cita.setFechaHoraInicio(Instant.parse("2026-08-05T15:00:00Z"));
		cita.setFechaHoraFin(Instant.parse("2026-08-05T15:30:00Z"));
		cita.setEstado(EstadoCita.CONFIRMADA);
		return cita;
	}

	private Profesional profesional() {
		return profesional(USUARIO_ID);
	}

	private Profesional profesional(UUID usuarioId) {
		Usuario usuario = new Usuario();
		usuario.setId(usuarioId);

		Profesional profesional = new Profesional();
		profesional.setId(PROFESIONAL_ID);
		profesional.setUsuario(usuario);
		return profesional;
	}

	private UsuarioDetails autenticar(List<String> roles, List<UUID> empresas, List<UUID> sucursales) {
		Usuario usuario = new Usuario();
		usuario.setId(USUARIO_ID);
		usuario.setEmail("test@visium.cl");
		usuario.setPasswordHash("hash");
		usuario.setActivo(true);
		usuario.setNombre("Test");
		usuario.setApellido("User");

		UsuarioDetails detalles = new UsuarioDetails(usuario, roles, empresas, sucursales);
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(detalles, null, detalles.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
		return detalles;
	}

	private UsuarioEmpresa pertenencia() {
		return pertenencia(EMPRESA_ID);
	}

	private UsuarioEmpresa pertenencia(UUID empresaId) {
		Empresa empresa = new Empresa();
		empresa.setId(empresaId);

		UsuarioEmpresa pertenencia = new UsuarioEmpresa();
		pertenencia.setEmpresa(empresa);
		return pertenencia;
	}

	private static List<String> roles(String... codigos) {
		return List.of(codigos);
	}
}