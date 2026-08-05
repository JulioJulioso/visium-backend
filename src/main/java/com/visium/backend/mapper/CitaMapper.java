package com.visium.backend.mapper;

import com.visium.backend.dto.cita.CitaRequest;
import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.entity.Cita;
import org.springframework.stereotype.Component;

@Component
public class CitaMapper {

	public CitaResponse toResponse(Cita cita) {
		if (cita == null) {
			return null;
		}

		return CitaResponse.builder()
				.id(cita.getId())
				.empresaId(cita.getEmpresaId())
				.sucursalId(cita.getSucursal() != null ? cita.getSucursal().getId() : null)
				.sucursalNombre(cita.getSucursal() != null ? cita.getSucursal().getNombre() : null)
				.pacienteId(cita.getPaciente() != null ? cita.getPaciente().getId() : null)
				.pacienteNombre(cita.getPaciente() != null ? cita.getPaciente().getNombre() : null)
				.pacienteApellido(cita.getPaciente() != null ? cita.getPaciente().getApellido() : null)
				.profesionalId(cita.getProfesional() != null ? cita.getProfesional().getId() : null)
				.fechaHoraInicio(cita.getFechaHoraInicio())
				.fechaHoraFin(cita.getFechaHoraFin())
				.estado(cita.getEstado())
				.motivo(cita.getMotivo())
				.observaciones(cita.getObservaciones())
				.createdAt(cita.getCreatedAt())
				.updatedAt(cita.getUpdatedAt())
				.build();
	}

	public Cita toEntity(CitaRequest request) {
		if (request == null) {
			return null;
		}

		Cita cita = new Cita();
		cita.setEmpresaId(request.getEmpresaId());
		cita.setFechaHoraInicio(request.getFechaHoraInicio());
		cita.setFechaHoraFin(request.getFechaHoraFin());
		cita.setEstado(request.getEstado());
		cita.setMotivo(request.getMotivo());
		cita.setObservaciones(request.getObservaciones());

		// Las relaciones (Sucursal, Paciente, Profesional) se obtienen de sus
		// respectivos repositorios en el CitaService y se asignan a la entidad.
		return cita;
	}

	public void updateEntityFromRequest(CitaRequest request, Cita cita) {
		if (request == null || cita == null) {
			return;
		}

		if (request.getEmpresaId() != null) {
			cita.setEmpresaId(request.getEmpresaId());
		}
		if (request.getFechaHoraInicio() != null) {
			cita.setFechaHoraInicio(request.getFechaHoraInicio());
		}
		if (request.getFechaHoraFin() != null) {
			cita.setFechaHoraFin(request.getFechaHoraFin());
		}
		if (request.getEstado() != null) {
			cita.setEstado(request.getEstado());
		}
		if (request.getMotivo() != null) {
			cita.setMotivo(request.getMotivo());
		}
		if (request.getObservaciones() != null) {
			cita.setObservaciones(request.getObservaciones());
		}
	}
}
