package br.com.cps.hae.web.api;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.institution.CourseService;
import br.com.cps.hae.application.institution.CourseService.CourseView;
import br.com.cps.hae.application.institution.InstitutionService;
import br.com.cps.hae.application.institution.InstitutionService.PublicInstitution;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Dados públicos necessários para a tela de cadastro. */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Público")
class PublicController {

    private final InstitutionService institutions;
    private final CourseService courses;

    PublicController(InstitutionService institutions, CourseService courses) {
        this.institutions = institutions;
        this.courses = courses;
    }

    @GetMapping("/institutions")
    ResponseEntity<List<PublicInstitution>> institutions() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)))
                .body(institutions.listPublic());
    }

    @GetMapping("/institutions/{code}/courses")
    ResponseEntity<List<CourseView>> courses(@PathVariable int code) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)))
                .body(courses.listPublic(code));
    }
}
