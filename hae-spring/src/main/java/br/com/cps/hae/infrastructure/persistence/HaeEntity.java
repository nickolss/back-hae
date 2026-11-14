package br.com.cps.hae.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.cps.hae.domain.hae.Dimension;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.hae.Modality;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "hae")
class HaeEntity {

    @Id
    UUID id;
    @Column(name = "employee_id")
    UUID employeeId;
    @Column(name = "institution_id")
    UUID institutionId;
    @Column(name = "course_id")
    UUID courseId;
    String title;
    String description;
    String observations;
    @Enumerated(EnumType.STRING)
    HaeType type;
    @Enumerated(EnumType.STRING)
    Modality modality;
    @Enumerated(EnumType.STRING)
    Dimension dimension;
    @Column(name = "weekly_hours")
    int weeklyHours;
    @Column(name = "start_date")
    LocalDate startDate;
    @Column(name = "end_date")
    LocalDate endDate;
    @Enumerated(EnumType.STRING)
    HaeStatus status;
    @Column(name = "reviewer_id")
    UUID reviewerId;
    @Column(name = "review_comment")
    String reviewComment;
    boolean viewed;
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;
    @Version
    Long version;

    @ElementCollection
    @CollectionTable(name = "hae_schedule", joinColumns = @JoinColumn(name = "hae_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "day_of_week")
    Map<DayOfWeek, Slot> schedule = new EnumMap<>(DayOfWeek.class);

    @ElementCollection
    @CollectionTable(name = "hae_student", joinColumns = @JoinColumn(name = "hae_id"))
    @OrderColumn(name = "position")
    @Column(name = "ra")
    List<String> students = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "hae_attachment", joinColumns = @JoinColumn(name = "hae_id"))
    @OrderBy("uploadedAt")
    List<AttachmentEmbeddable> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "hae", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    List<ClosureEntity> closures = new ArrayList<>();

    @Embeddable
    static class Slot {
        @Column(name = "start_time")
        LocalTime start;
        @Column(name = "end_time")
        LocalTime end;
    }

    @Embeddable
    static class AttachmentEmbeddable {
        UUID id;
        @Column(name = "file_name")
        String fileName;
        @Column(name = "content_type")
        String contentType;
        @Column(name = "size_bytes")
        long sizeBytes;
        String url;
        @Column(name = "storage_key")
        String storageKey;
        @Column(name = "uploaded_at")
        Instant uploadedAt;
    }
}
