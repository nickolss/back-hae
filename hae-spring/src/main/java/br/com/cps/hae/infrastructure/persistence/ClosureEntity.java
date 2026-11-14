package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.hae.ReviewDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "hae_closure")
class ClosureEntity {

    @Id
    UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hae_id")
    HaeEntity hae;
    int position;
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type")
    HaeType reportType;
    String role;
    @Column(name = "student_count")
    Integer studentCount;
    @Column(name = "student_names")
    String studentNames;
    @Column(name = "approved_students")
    String approvedStudents;
    @Column(name = "project_info")
    String projectInfo;
    @Column(name = "student_info")
    String studentInfo;
    @Column(name = "support_type")
    String supportType;
    String description;
    @Column(name = "certificate_students")
    String certificateStudents;
    @Column(name = "submitted_at")
    Instant submittedAt;
    @Enumerated(EnumType.STRING)
    ReviewDecision decision;
    @Column(name = "reviewer_id")
    UUID reviewerId;
    @Column(name = "review_comment")
    String reviewComment;
    @Column(name = "decided_at")
    Instant decidedAt;
}
