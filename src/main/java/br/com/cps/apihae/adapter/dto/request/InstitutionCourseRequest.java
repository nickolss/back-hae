package br.com.cps.apihae.adapter.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionCourseRequest {
    @NotBlank(message = "A instituição é obrigatória")
    private String institutionId;

    @NotBlank(message = "O nome do curso é obrigatório")
    private String courseName;
}
