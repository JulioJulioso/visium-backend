package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.consulta.CerrarCitaConsultaRequest;
import com.visium.backend.dto.consulta.ConsultaResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Paciente;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.enums.EstadoCita;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.mapper.ConsultaMapper;
import com.visium.backend.repository.CitaRepository;
import com.visium.backend.repository.ConsultaRepository;
import com.visium.backend.repository.PacienteRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

	private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID SUCURSAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID PACIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID PROFESIONAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID CITA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	@Mock
	private ConsultaRepository consultaRepository;

	@Mock
	private CitaRepository citaRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private AccesoService accesoService;

	private ConsultaService consultaService;

	@BeforeEach
	void setUp() {
		consultaService = new ConsultaService(
				consultaRepository,
				citaRepository,
				pacienteRepository,
				new ConsultaMapper(),
				accesoService);
	}

	@Test
	void cerrarCitaConfirmadaCreaConsultaYCambiaEstadoAAtendida() {
		Cita cita = cita(EstadoCita.CONFIRMADA);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita));
		when(consultaRepository.findByCitaId(CITA_ID)).thenReturn(Optional.empty());
		when(consultaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ConsultaResponse response = consultaService.cerrarCita(request());

		assertEquals(EstadoCita.ATENDIDA, cita.getEstado());
		assertEquals(EstadoCita.ATENDIDA, response.getEstadoCita());
		assertEquals(CITA_ID, response.getCitaId());
		assertEquals(PACIENTE_ID, response.getPacienteId());
		assertNotNull(response.getFechaFin());
		verify(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);
		verify(citaRepository).save(cita);
	}

	@Test
	void cerrarCitaNoConfirmadaFallaYNoCreaConsulta() {
		Cita cita = cita(EstadoCita.PENDIENTE);
		when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita));

		assertThrows(BadRequestException.class, () -> consultaService.cerrarCita(request()));

		assertEquals(EstadoCita.PENDIENTE, cita.getEstado());
		verify(consultaRepository, never()).save(any());
		verify(citaRepository, never()).save(any());
	}

	private CerrarCitaConsultaRequest request() {
		CerrarCitaConsultaRequest request = new CerrarCitaConsultaRequest();
		request.setCitaId(CITA_ID);
		request.setMotivoConsulta("Control visual");
		request.setAnamnesis("Paciente sin antecedentes relevantes");
		request.setExamenVisual("Agudeza visual conservada");
		request.setDiagnostico("Control de rutina");
		request.setObservaciones("Sin observaciones");
		return request;
	}

	private Cita cita(EstadoCita estado) {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);

		Sucursal sucursal = new Sucursal();
		sucursal.setId(SUCURSAL_ID);
		sucursal.setEmpresa(empresa);

		Paciente paciente = new Paciente();
		paciente.setId(PACIENTE_ID);
		paciente.setEmpresa(empresa);

		Profesional profesional = new Profesional();
		profesional.setId(PROFESIONAL_ID);

		Cita cita = new Cita();
		cita.setId(CITA_ID);
		cita.setEmpresaId(EMPRESA_ID);
		cita.setSucursal(sucursal);
		cita.setPaciente(paciente);
		cita.setProfesional(profesional);
		cita.setEstado(estado);
		return cita;
	}
}
