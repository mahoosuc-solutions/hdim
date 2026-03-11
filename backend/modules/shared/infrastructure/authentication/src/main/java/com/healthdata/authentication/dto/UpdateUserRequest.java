package com.healthdata.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(max = 100) private String firstName;
    @Size(max = 100) private String lastName;
    @Email @Size(max = 100) private String email;
    @Size(max = 500) private String notes;
}
