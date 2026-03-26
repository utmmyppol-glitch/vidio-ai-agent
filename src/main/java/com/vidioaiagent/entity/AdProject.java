package com.vidioaiagent.entity;

import com.vidioaiagent.enums.Platform;
import com.vidioaiagent.enums.ProjectStatus;
import com.vidioaiagent.enums.Style;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ad_projects")
public class AdProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    @Column(columnDefinition = "LONGTEXT")
    private String productDescription;

    @Column(nullable = false)
    private String targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Style adStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PENDING;

    @Builder.Default
    private Integer progressPercent = 0;

    private String additionalRequest;

    @Column(columnDefinition = "LONGTEXT")
    private String trendAnalysis;

    @Column(columnDefinition = "LONGTEXT")
    private String adCopy;

    @Column(columnDefinition = "LONGTEXT")
    private String script;

    @Column(columnDefinition = "LONGTEXT")
    private String hashtags;

    @Column(columnDefinition = "LONGTEXT")
    private String hookText;

    @Column(columnDefinition = "LONGTEXT")
    private String subtitles;

    private String videoUrl;
    private String thumbnailUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "adProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GeneratedContent> generatedContents = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
