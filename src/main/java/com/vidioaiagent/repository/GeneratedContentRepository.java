package com.vidioaiagent.repository;

import com.vidioaiagent.entity.GeneratedContent;
import com.vidioaiagent.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {
    List<GeneratedContent> findByAdProjectId(Long adProjectId);
    List<GeneratedContent> findByAdProjectIdAndContentType(Long adProjectId, ContentType contentType);
}
