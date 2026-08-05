package com.visium.backend.controller;

import com.visium.backend.dto.dashboard.DashboardResumenResponse;
import com.visium.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard: totales y proximas citas calculados desde la base de datos
 * para la empresa activa y las sucursales autorizadas del usuario.
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/resumen")
	@Operation(
			summary = "Resumen del dashboard",
			description = "REQUIERE token JWT (cualquier rol autenticado). "
					+ "Devuelve totales (pacientes, citas confirmadas de hoy) y las proximas "
					+ "5 citas confirmadas, calculados desde la base de datos. "
					+ "Se usa la empresa activa (header X-Empresa-Id o unica del usuario) "
					+ "y se filtran las sucursales autorizadas del usuario.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<DashboardResumenResponse> resumen() {
		return ResponseEntity.ok(dashboardService.resumen());
	}
}
