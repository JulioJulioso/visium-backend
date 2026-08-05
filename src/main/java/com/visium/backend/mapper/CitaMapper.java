package com.visium.backend.mapper;

import com.visium.backend.dto.cita.CitaResponse;
import com.visium.backend.entity.Cita;
import org.springframework.stereotype.Component;

@Component
public class CitaMapper {

	public CitaResponse toResponse(Cita cita) {
		return CitaResponse.builder()
				.id(cita.getId())
				.empresaId(cita.getEmpresaId())
				.sucursalId(cita.getSucursal().getId())
				.sucursalNombre(cita.getSucursal().getNombre())
				.pacienteId(cita.getPaciente().getId())
				.pacienteNombre(cita.getPaciente().getNombre())
				.pacienteApellido(cita.getPaciente().getApellido())
				.profesionalId(cita.getProfesional().getId())
				.fechaHoraInicio(cita.getFechaHoraInicio())
				.fechaHoraFin(cita.getFechaHoraFin())
				.estado(cita.getEstado())
				.motivo(cita.getMotivo())
				.observaciones(cita.getObservaciones())
				.createdAt(cita.getCreatedAt())
				.updatedAt(cita.getUpdatedAt())
				.build();
	}
}
