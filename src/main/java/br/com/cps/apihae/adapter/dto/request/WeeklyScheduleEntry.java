package br.com.cps.apihae.adapter.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class WeeklyScheduleEntry {
    @NotBlank(message = "O intervalo de tempo não pode estar em branco.")
    private String timeRange;
}