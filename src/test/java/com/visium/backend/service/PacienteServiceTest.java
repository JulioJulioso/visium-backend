package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.paciente.PacienteHistorialResponse;
import com.visium.backend.dto.paciente.PacientePageResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Consulta;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.RecetaOptica;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.entity.Usuario;
import com.visium.backend.enums.TipoHistorial;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.mapper.PacienteMapper;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.FichaClinicaRepository;
import com.visium.backend.repository.PacienteRepository;
import com.visium.backend.repository.RecetaOpticaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

	private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID SUCURSAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID PACIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID CITA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final UUID CONSULTA_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	private static final UUID RECETA_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

	@Mock private PacienteRepository pacienteRepository;
	@Mock private EmpresaRepository empresaRepository;
	@Mock private FichaClinicaRepository fichaClinicaRepository;
	@Mock private ConsultaRepository consultaRepository;
	@Mock private RecetaOpticaRepository recetaOpticaRepository;
	@Mock private AccesoService accesoService;
	@Mock private PacienteMapper pacienteMapper;

	private PacienteService service;

	@BeforeEach
	void setUp() {
		service =
				new PacienteService(
						pacienteRepository,
						empresaRepository,
						fichaClinicaRepository,
						consultaRepository,
						recetaOpticaRepository,
						pacienteMapper,
						accesoService);
	}

	// ---------- Busqueda paginada ----------

	@Test
	void listarPaginado_delegaConTextoYPageable() {
		Paciente paciente = new Paciente();
		paciente.setId(PACIENTE_ID);
		Page<Paciente> pagina = new PageImpl<>(List.of(paciente), Pageable.ofSize(20), 1);

		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		when(pacienteRepository.buscarPorEmpresa(eq(EMPRESA_ID), eq("juan"), any(Pageable.class)))
				.thenReturn(pagina);
		when(pacienteMapper.toResponse(paciente)).thenReturn(null);

		PacientePageResponse resultado = service.listarPaginado(EMPRESA_ID, "juan", 0, 20);

		assertEquals(0, resultado.getPage());
		assertEquals(20, resultado.getSize());
		assertEquals(1, resultado.getTotalElements());
		assertEquals(1, resultado.getTotalPages());
		assertEquals(1, resultado.getContent().size());
		assertNull(resultado.getContent().getFirst());
		verify(accesoService).resolverEmpresaObjetivo(EMPRESA_ID);
	}

	@Test
	void listarPaginado_limitaTamanoMaximoYNoPermitePaginasNegativas() {
		Page<Paciente> pagina = new PageImpl<>(List.of(), Pageable.ofSize(100), 0);
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		when(pacienteRepository.buscarPorEmpresa(eq(EMPRESA_ID), eq(null), any(Pageable.class)))
				.thenReturn(pagina);

		service.listarPaginado(EMPRESA_ID, null, -3, 5000);

		verify(pacienteRepository).buscarPorEmpresa(eq(EMPRESA_ID), eq(null), any(Pageable.class));
	}

	// ---------- Historial ----------

	@Test
	void historial_combinaConsultasYRecetasOrdenadasPorFechaDescendente() {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		Paciente paciente = new Paciente();
		paciente.setId(PACIENTE_ID);
		paciente.setEmpresa(empresa);

		Sucursal sucursal = new Sucursal();
		sucursal.setId(SUCURSAL_ID);

		Usuario usuario = new Usuario();
		usuario.setNombre("Ana");
		usuario.setApellido("Lopez");
		Profesional profesional = new Profesional();
		profesional.setUsuario(usuario);

		Cita cita = new Cita();
		cita.setId(CITA_ID);
		cita.setSucursal(sucursal);
		cita.setProfesional(profesional);

		Consulta consulta = new Consulta();
		consulta.setId(CONSULTA_ID);
		consulta.setCita(cita);
		consulta.setDiagnostico("Miopia");
		consulta.setFechaInicio(Instant.parse("2026-01-01T10:00:00Z"));

		RecetaOptica receta = new RecetaOptica();
		receta.setId(RECETA_ID);
		receta.setConsulta(consulta);
		receta.setIndicaciones("Uso diario");
		receta.setCreatedAt(Instant.parse("2026-02-01T10:00:00Z"));

		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of(SUCURSAL_ID));
		when(consultaRepository.findByCitaPacienteIdOrderByCreatedAtDesc(PACIENTE_ID))
				.thenReturn(List.of(consulta));
		when(recetaOpticaRepository.findHistorialByPacienteId(PACIENTE_ID))
				.thenReturn(List.of(receta));

		List<PacienteHistorialResponse> historial = service.historial(PACIENTE_ID);

		assertEquals(2, historial.size());
		assertEquals(TipoHistorial.RECETA, historial.get(0).getTipo());
		assertEquals(RECETA_ID, historial.get(0).getId());
		assertEquals("Ana Lopez", historial.get(0).getProfesionalNombre());
		assertEquals(TipoHistorial.CONSULTA, historial.get(1).getTipo());
		assertEquals("Miopia", historial.get(1).getDetalle());
	}

	@Test
	void historial_filtraRegistrosDeSucursalesNoAutorizadas() {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		Paciente paciente = new Paciente();
		paciente.setId(PACIENTE_ID);
		paciente.setEmpresa(empresa);

		Sucursal otraSucursal = new Sucursal();
		otraSucursal.setId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

		Cita cita = new Cita();
		cita.setId(CITA_ID);
		cita.setSucursal(otraSucursal);
		cita.setProfesional(new Profesional());

		Consulta consulta = new Consulta();
		consulta.setId(CONSULTA_ID);
		consulta.setCita(cita);
		consulta.setFechaInicio(Instant.now());

		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of(SUCURSAL_ID));
		when(consultaRepository.findByCitaPacienteIdOrderByCreatedAtDesc(PACIENTE_ID))
				.thenReturn(List.of(consulta));
		when(recetaOpticaRepository.findHistorialByPacienteId(PACIENTE_ID)).thenReturn(List.of());

		List<PacienteHistorialResponse> historial = service.historial(PACIENTE_ID);

		assertEquals(0, historial.size());
	}

	@Test
	void historial_lanzaSiPacienteNoExiste() {
		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.historial(PACIENTE_ID));
		verify(accesoService, never()).exigirAccesoEmpresa(any());
	}

	@Test
	void historial_lanzaSiNoTieneAccesoALaEmpresa() {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		Paciente paciente = new Paciente();
		paciente.setId(PACIENTE_ID);
		paciente.setEmpresa(empresa);

		when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
		org.mockito.Mockito.doThrow(new ForbiddenException("No tienes acceso a esa empresa"))
				.when(accesoService)
				.exigirAccesoEmpresa(EMPRESA_ID);

		assertThrows(ForbiddenException.class, () -> service.historial(PACIENTE_ID));
		verify(consultaRepository, never()).findByCitaPacienteIdOrderByCreatedAtDesc(any());
	}
}
