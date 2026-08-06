package com.visium.backend.controller;

import com.visium.backend.dto.rol.RolRequest;
import com.visium.backend.dto.rol.RolResponse;
import com.visium.backend.service.RolService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RolController {
  private final RolService rolService;
  @GetMapping public ResponseEntity<List<RolResponse>> listar() { return ResponseEntity.ok(rolService.listar()); }
  @PostMapping public ResponseEntity<RolResponse> crear(@Valid @RequestBody RolRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(request)); }
  @PutMapping("/{id}") public ResponseEntity<RolResponse> actualizar(@PathVariable Short id, @Valid @RequestBody RolRequest request) { return ResponseEntity.ok(rolService.actualizar(id, request)); }
  @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Short id) { rolService.eliminar(id); return ResponseEntity.noContent().build(); }
}
