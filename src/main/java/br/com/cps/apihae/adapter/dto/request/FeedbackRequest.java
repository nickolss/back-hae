package br.com.cps.apihae.adapter.dto.request;

import br.com.cps.apihae.domain.enums.FeedbackCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackRequest {
    private String name;
    private String email;
    private String subject;
    private FeedbackCategory category;
    private String description;
}
