package com.dmh.users.exception;

import java.lang.module.ModuleDescriptor.Builder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dmh.users.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex){
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField, 
						FieldError::getDefaultMessage,
						(first, second) -> first,
						LinkedHashMap::new));
		return build(HttpStatus.BAD_REQUEST, "Hay datos invalidos", errors);
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex){
		return Build(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion falta o no es un JSON valido",
				Map.of());
	}
	
	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex){
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
	}
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex){
		return build(HttpStatus.BAD_REQUEST, "El mail o el dni ya estan registrados", Map.of());
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex){
		log.error("Error inesperado", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", Map.of());
	}
	
	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> errors){
		return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, errors));
	}
	

}
