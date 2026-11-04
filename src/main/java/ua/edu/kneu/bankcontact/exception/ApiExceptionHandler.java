package ua.edu.kneu.bankcontact.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.net.URI;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException ex,
                                           HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> noRoute(NoResourceFoundException ex,
                                          HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "API route was not found", request);
    }
    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ResourceConflictException ex,
                                           HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException ex,
                                            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT,
            "A unique code already exists or the record is still referenced", request);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex,
                                             HttpServletRequest request) {
        ProblemDetail body = body(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        var errors = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
    @ExceptionHandler({ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalidInput(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid JSON, identifier or query parameter", request);
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> unsupportedType(HttpMediaTypeNotSupportedException ex,
                                                 HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use Content-Type application/json", request);
    }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail,
                                                  HttpServletRequest request) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(body(status, detail, request));
    }
    private ProblemDetail body(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(request.getRequestURI()));
        return body;
    }
}
