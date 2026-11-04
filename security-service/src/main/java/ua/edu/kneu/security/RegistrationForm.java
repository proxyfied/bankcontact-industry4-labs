package ua.edu.kneu.security;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class RegistrationForm {
 @NotBlank(message="Введіть логін.")
 @Pattern(regexp="[A-Za-z0-9._-]{3,40}",message="Логін: 3–40 латинських літер, цифр або . _ -.")
 private String username="";
 @NotBlank(message="Введіть пароль.")
 @Size(min=8,max=72,message="Пароль має містити від 8 до 72 символів.")
 private String password="";
 @NotBlank(message="Повторіть пароль.") private String confirmation="";
}
