package com.codemind.repo_service.dto;

import com.codemind.repo_service.model.RepoStatus;
import com.codemind.repo_service.model.SourceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RepoResponse {
    private UUID id;
    private String githubUrl;
    private String name;
    private String owner;
    private SourceType sourceType;
    private RepoStatus status;
    private LocalDateTime createdAt;
}