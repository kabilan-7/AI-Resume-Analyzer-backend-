package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "screening_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String jobTitle;
    private String classification;
    private int score;
    private int yearsExperience;

    @ElementCollection
    @CollectionTable(name = "matched_skills",
            joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "skill")
    private List<String> matchedSkills;

    @ElementCollection
    @CollectionTable(name = "missing_skills",
            joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "skill")
    private List<String> missingSkills;

    @Column(length = 2000)
    private String summary;

    private Instant createdAt;

    // ── NEW — links each screening to the recruiter who created it ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}