package com.visium.backend.mapper;

import com.visium.backend.dto.consulta.ConsultaRequest;
import com.visium.backend.dto.consulta.ConsultaResponse;
import com.visium.backend.entity.Cita;
import com.visium.backend.entity.Consulta;
import org.springframework.stereotype.Component;

@Component
public class ConsultaMapper {

	public Consulta toEntity(ConsultaRequest request, Cita cita) {
		Consulta consulta = new Consulta();
		consulta.setCita(cita);
		aplicar(consulta, request);
		return consulta;
	}

	public void aplicar(Consulta consulta, ConsultaRequest request) {
		consulta.setMotivoConsulta(request.getMotivoConsulta());
		consulta.setAnamnesis(request.getAnamnesis());
		consulta.setExamenVisual(request.getExamenVisual());
		consulta.setDiagnostico(request.getDiagnostico());
		consulta.setObservaciones(request.getObservaciones());
	}

	public ConsultaResponse toResponse(Consulta consulta) {
		Cita cita = consulta.getCita();
		return ConsultaResponse.builder()
				.id(consulta.getId())
				.citaId(cita.getId())
				.empresaId(cita.getEmpresaId())
				.sucursalId(cita.getSucursal().getId())
				.pacienteId(cita.getPaciente().getId())
				.profesionalId(cita.getProfesional().getId())
				.estadoCita(cita.getEstado())
				.motivoConsulta(consulta.getMotivoConsulta())
				.anamnesis(consulta.getAnamnesis())
				.examenVisual(consulta.getExamenVisual())
				.diagnostico(consulta.getDiagnostico())
				.observaciones(consulta.getObservaciones())
				.fechaInicio(consulta.getFechaInicio())
				.fechaFin(consulta.getFechaFin())
				.createdAt(consulta.getCreatedAt())
				.updatedAt(consulta.getUpdatedAt())
				.build();
	}
}
