package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "job_openings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ElementCollection
    @CollectionTable(
            name = "job_required_skills",
            joinColumns = @JoinColumn(name = "job_id")
    )
    @Column(name = "skill")
    private List<String> requiredSkills;

    private int minYearsExperience;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    // ── Who created this job opening ──────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (status == null) {
            status = JobStatus.OPEN;
        }
    }
}