package com.visium.backend.service;

import com.visium.backend.dto.rol.RolRequest;
import com.visium.backend.dto.rol.RolResponse;
import com.visium.backend.entity.Rol;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.RolRepository;
import com.visium.backend.repository.UsuarioEmpresaRolRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolService {
  private final RolRepository rolRepository;
  private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;

  @Transactional(readOnly = true)
  public List<RolResponse> listar() {
    return rolRepository.findAll().stream()
        .filter(rol -> rol.getCodigo() != null && !rol.getCodigo().isBlank()
            && rol.getNombre() != null && !rol.getNombre().isBlank())
        .map(this::toResponse).toList();
  }

  @Transactional
  public RolResponse crear(RolRequest request) {
    String codigo = request.getCodigo().trim();
    if (rolRepository.findByCodigo(codigo).isPresent()) throw new BadRequestException("Ya existe el rol " + codigo);
    Rol rol = new Rol(); rol.setCodigo(codigo); rol.setNombre(request.getNombre().trim());
    return toResponse(rolRepository.save(rol));
  }

  @Transactional
  public RolResponse actualizar(Short id, RolRequest request) {
    Rol rol = buscar(id); String codigo = request.getCodigo().trim();
    rolRepository.findByCodigo(codigo).filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> { throw new BadRequestException("Ya existe el rol " + codigo); });
    rol.setCodigo(codigo); rol.setNombre(request.getNombre().trim()); return toResponse(rolRepository.save(rol));
  }

  @Transactional
  public void eliminar(Short id) {
    Rol rol = buscar(id);
    if (usuarioEmpresaRolRepository.existsByRolId(id)) throw new BadRequestException("No se puede eliminar un rol que está asignado a usuarios");
    rolRepository.delete(rol);
  }

  private Rol buscar(Short id) { return rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + id)); }
  private RolResponse toResponse(Rol rol) { return RolResponse.builder().id(rol.getId()).codigo(rol.getCodigo()).nombre(rol.getNombre()).build(); }
}
