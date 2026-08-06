package com.visium.backend.mapper;

import com.visium.backend.dto.paciente.PacienteRequest;
import com.visium.backend.dto.paciente.PacienteResponse;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Paciente;
import com.visium.backend.enums.TipoDocumento;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {

	public Paciente toEntity(PacienteRequest request, Empresa empresa) {
		Paciente paciente = new Paciente();
		paciente.setEmpresa(empresa);
		aplicar(paciente, request);
		return paciente;
	}

	public void aplicar(Paciente paciente, PacienteRequest request) {
		paciente.setTipoDocumento(
				request.getTipoDocumento() != null ? request.getTipoDocumento() : TipoDocumento.RUN
		);
		paciente.setNumeroDocumento(request.getNumeroDocumento());
		paciente.setSucursalId(request.getSucursalId());
		paciente.setNombre(request.getNombre());
		paciente.setApellido(request.getApellido());
		paciente.setFechaNacimiento(request.getFechaNacimiento());
		paciente.setSexo(request.getSexo());
		paciente.setTelefono(request.getTelefono());
		paciente.setEmail(request.getEmail());
		paciente.setDireccion(request.getDireccion());
		if (request.getActivo() != null) {
			paciente.setActivo(request.getActivo());
		}
	}

	public PacienteResponse toResponse(Paciente paciente) {
		return PacienteResponse.builder()
				.id(paciente.getId())
				.empresaId(paciente.getEmpresa().getId())
				.sucursalId(paciente.getSucursalId())
				.tipoDocumento(paciente.getTipoDocumento())
				.numeroDocumento(paciente.getNumeroDocumento())
				.nombre(paciente.getNombre())
				.apellido(paciente.getApellido())
				.fechaNacimiento(paciente.getFechaNacimiento())
				.sexo(paciente.getSexo())
				.telefono(paciente.getTelefono())
				.email(paciente.getEmail())
				.direccion(paciente.getDireccion())
				.activo(paciente.getActivo())
				.createdAt(paciente.getCreatedAt())
				.updatedAt(paciente.getUpdatedAt())
				.build();
	}
}
