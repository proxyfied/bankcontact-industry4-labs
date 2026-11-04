package ua.edu.kneu.bi;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
@RestControllerAdvice
public class BiExceptionHandler {
 @ExceptionHandler(ImportProblem.class) ResponseEntity<ProblemDetail> invalid(ImportProblem ex) { return problem(ex.status(),ex.getMessage()); }
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<ProblemDetail> size(Exception ex) { return problem(413,"Файл перевищує 2 МБ."); }
 @ExceptionHandler({MissingServletRequestPartException.class,MultipartException.class}) ResponseEntity<ProblemDetail> multipart(Exception ex) { return problem(400,"Потрібен файл у multipart-полі file."); }
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ProblemDetail> conflict(Exception ex) { return problem(409,"Конфлікт кодів записів. Імпорт скасовано повністю."); }
 private ResponseEntity<ProblemDetail> problem(int status,String detail) {
  return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status),detail));
 }
}
