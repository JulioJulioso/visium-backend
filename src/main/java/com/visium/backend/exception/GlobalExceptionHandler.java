package com.visium.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Convierte excepciones en respuestas JSON claras para el frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		String mensaje = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Datos invalidos");
		return build(HttpStatus.BAD_REQUEST, mensaje);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, Object>> handleParametroFaltante(
			MissingServletRequestParameterException ex) {
		return build(
				HttpStatus.BAD_REQUEST,
				"Falta el parametro requerido: " + ex.getParameterName());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNoEncontradoRecurso(NoResourceFoundException ex) {
		return build(HttpStatus.NOT_FOUND, "Recurso no encontrado: " + ex.getResourcePath());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTipoInvalido(
			MethodArgumentTypeMismatchException ex) {
		return build(
				HttpStatus.BAD_REQUEST,
				"Valor invalido para el parametro " + ex.getName() + ": " + ex.getValue());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, Object>> handleMetodoNoSoportado(
			HttpRequestMethodNotSupportedException ex) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no soportado para esta ruta");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleInesperada(Exception ex) {
		// No loguear mensaje completo: evita exponer detalles internos al cliente.
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
	}

	private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje) {
		Map<String, Object> body = new HashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", mensaje);
		return ResponseEntity.status(status).body(body);
	}
}
