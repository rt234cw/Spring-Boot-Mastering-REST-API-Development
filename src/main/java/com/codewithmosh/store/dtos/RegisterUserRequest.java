package com.codewithmosh.store.dtos;


import com.codewithmosh.store.validation.Lowercase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    @NotBlank(message = "Name is required")
    @Size(max=255, message = "Too long")
    private String name;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 25, message = "At least 6 characters")

    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Lowercase(message = "Email must be in lowercase")
    private String email;

}
