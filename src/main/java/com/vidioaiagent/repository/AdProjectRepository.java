package com.vidioaiagent.repository;

import com.vidioaiagent.entity.AdProject;
import com.vidioaiagent.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdProjectRepository extends JpaRepository<AdProject, Long> {
    List<AdProject> findByStatusOrderByCreatedAtDesc(ProjectStatus status);
    List<AdProject> findAllByOrderByCreatedAtDesc();
}
