package br.com.cps.apihae.adapter.dto.response;

import br.com.cps.apihae.domain.entity.Institution;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InstitutionResponseDTO {
    private String id;
    private String name;
    private Integer institutionCode;
    private String address;

    public InstitutionResponseDTO(Institution institution) {
        this.id = institution.getId();
        this.name = institution.getName();
        this.institutionCode = institution.getInstitutionCode();
        this.address = institution.getAddress();
    }
}
