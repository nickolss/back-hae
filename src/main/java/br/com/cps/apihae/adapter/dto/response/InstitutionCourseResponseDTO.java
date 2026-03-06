package br.com.cps.apihae.adapter.dto.response;

import br.com.cps.apihae.domain.entity.InstitutionCourse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InstitutionCourseResponseDTO {
    private String id;
    private String courseName;
    private String institutionId;
    private Integer institutionCode;
    private String institutionName;

    public InstitutionCourseResponseDTO(InstitutionCourse institutionCourse) {
        this.id = institutionCourse.getId();
        this.courseName = institutionCourse.getCourseName();
        this.institutionId = institutionCourse.getInstitution().getId();
        this.institutionCode = institutionCourse.getInstitution().getInstitutionCode();
        this.institutionName = institutionCourse.getInstitution().getName();
    }
}
