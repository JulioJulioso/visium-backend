package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.entity.Usuario;
import com.visium.backend.entity.UsuarioEmpresa;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.mapper.CitaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.ProfesionalRepository;
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
	private static final UUID SUCURSAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID PROFESIONAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID USUARIO_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
	private static final UUID PACIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private CitaRepository citaRepository;

	@Mock
	private ProfesionalRepository profesionalRepository;

	@Mock
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Mock
	private AccesoService accesoService;

	private CitaService citaService;

	@BeforeEach
	void setUp() {
		citaService = new CitaService(
				citaRepository,
				profesionalRepository,
				usuarioEmpresaRepository,
				new CitaMapper(),
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

		List<CitaResponse> citas = citaService.listarCitasConfirmadasPorProfesional(
				PROFESIONAL_ID, null, null);

		assertEquals(1, citas.size());
		verify(citaRepository).findByProfesionalIdAndEstadoOrderByFechaHoraInicioAsc(
				PROFESIONAL_ID, EstadoCita.CONFIRMADA);
	}

	@Test
	void rangoInvalidoFalla() {
		autenticar(roles("JEFE"), List.of(EMPRESA_ID), List.of());

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
		Usuario usuario = new Usuario();
		usuario.setId(USUARIO_ID);

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
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);

		UsuarioEmpresa pertenencia = new UsuarioEmpresa();
		pertenencia.setEmpresa(empresa);
		return pertenencia;
	}

	private static List<String> roles(String... codigos) {
		return List.of(codigos);
	}
}
