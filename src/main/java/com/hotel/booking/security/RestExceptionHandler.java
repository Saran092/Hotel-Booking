package com.hotel.booking.security;

import com.hotel.booking.service.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps exceptions to the JSON error envelope used by the Angular app. */
@RestControllerAdvice
public class RestExceptionHandler {

    private final Validator validator;

    public RestExceptionHandler(Validator validator) {
        this.validator = validator;
    }

    /** Runs bean validation plus the service-level checks that need a BindingResult. */
    public BeanPropertyBindingResult validate(Object form, Class<?>... groups) {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(form, "form");
        if (groups.length > 0) {
            validator.validate(form, result);
        } else {
            validator.validate(form, result);
        }
        return result;
    }

    public static Map<String, String> fieldErrors(BeanPropertyBindingResult result) {
        Map<String, String> map = new LinkedHashMap<>();
        for (FieldError fe : result.getFieldErrors()) {
            map.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return map;
    }

    public static void throwIfInvalid(BeanPropertyBindingResult result) {
        if (result.hasErrors()) throw new ApiValidationException(fieldErrors(result));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> business(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ApiValidationException.class)
    public ResponseEntity<Map<String, Object>> validation(ApiValidationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        body.put("errors", ex.getErrors());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> beanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Please correct the errors below.");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "The request could not be read. Please check your input."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> mismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid value for '" + ex.getName() + "'."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You do not have permission to do that."));
    }
}
