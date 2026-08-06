package com.visium.backend.service;

import com.visium.backend.dto.profesional.ProfesionalRequest;
import com.visium.backend.dto.profesional.ProfesionalResponse;
import com.visium.backend.entity.Profesional;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.ProfesionalRepository;
import com.visium.backend.repository.SucursalRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profesionales son datos clínicos, no cuentas de acceso al sistema. */
@Service @RequiredArgsConstructor
public class ProfesionalService {
  private final ProfesionalRepository profesionalRepository;
  private final EmpresaRepository empresaRepository;
  private final SucursalRepository sucursalRepository;
  private final AccesoService accesoService;

  @Transactional(readOnly = true) public List<ProfesionalResponse> listar(UUID empresaId, UUID sucursalId) {
    UUID empresa = accesoService.resolverEmpresaObjetivo(empresaId);
    if (sucursalId != null) accesoService.exigirAccesoSucursal(empresa, sucursalId);
    return profesionalRepository.findByEmpresaId(empresa).stream().filter(p -> sucursalId == null || sucursalId.equals(p.getSucursalId())).filter(this::visible).map(this::response).toList();
  }
  @Transactional(readOnly = true) public ProfesionalResponse obtenerPorId(UUID id) { Profesional p = buscar(id); exigirVisible(p); return response(p); }
  @Transactional public ProfesionalResponse registrar(ProfesionalRequest r) { validarGestion(); UUID empresa = validarEmpresaYSucursal(r); Profesional p = new Profesional(); aplicar(p,r,empresa); return response(profesionalRepository.save(p)); }
  @Transactional public ProfesionalResponse editar(UUID id, ProfesionalRequest r) { validarGestion(); Profesional p=buscar(id); exigirVisible(p); UUID empresa=validarEmpresaYSucursal(r); aplicar(p,r,empresa); return response(profesionalRepository.save(p)); }
  @Transactional public void cambiarEstado(UUID id, boolean activo) { validarGestion(); Profesional p=buscar(id); exigirVisible(p); p.setActivo(activo); profesionalRepository.save(p); }
  private void validarGestion(){ if(!accesoService.esSuperAdmin()&&!accesoService.esJefeDeEmpresa()&&!accesoService.esJefeSucursal()) throw new ForbiddenException("No puedes gestionar profesionales"); }
  private UUID validarEmpresaYSucursal(ProfesionalRequest r){ UUID e=accesoService.resolverEmpresaObjetivo(r.getEmpresaId()); empresaRepository.findById(e).orElseThrow(()->new ResourceNotFoundException("Empresa no encontrada")); UUID s=r.getSucursalIds().getFirst(); Sucursal suc=sucursalRepository.findById(s).orElseThrow(()->new ResourceNotFoundException("Sucursal no encontrada")); if(!e.equals(suc.getEmpresa().getId())) throw new ForbiddenException("La sucursal no pertenece a la empresa"); accesoService.exigirAccesoSucursal(e,s); return e; }
  private void aplicar(Profesional p, ProfesionalRequest r, UUID e){ p.setUsuario(null); p.setEmpresaId(e); p.setSucursalId(r.getSucursalIds().getFirst()); p.setNombre(r.getNombre()); p.setApellido(r.getApellido()); p.setEmail(r.getEmail()); p.setRun(r.getRun()); p.setTelefono(r.getTelefono()); p.setNumeroRegistro(r.getNumeroRegistro()); p.setEspecialidad(r.getEspecialidad()); if(p.getActivo()==null)p.setActivo(true); }
  private boolean visible(Profesional p){ if(!accesoService.puedeAccederEmpresa(p.getEmpresaId()))return false; List<UUID> ids=accesoService.sucursalIdsVisiblesEnEmpresa(); return ids.isEmpty()||ids.contains(p.getSucursalId()); }
  private void exigirVisible(Profesional p){if(!visible(p))throw new ForbiddenException("No tienes acceso a este profesional");}
  private Profesional buscar(UUID id){return profesionalRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Profesional no encontrado"));}
  private ProfesionalResponse response(Profesional p){return ProfesionalResponse.builder().id(p.getId()).empresaId(p.getEmpresaId()).nombre(p.getNombre()).apellido(p.getApellido()).email(p.getEmail()).numeroRegistro(p.getNumeroRegistro()).especialidad(p.getEspecialidad()).activo(p.getActivo()).sucursalIds(p.getSucursalId()==null?List.of():List.of(p.getSucursalId())).build();}
}
