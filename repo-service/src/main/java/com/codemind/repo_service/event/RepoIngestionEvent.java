package com.codemind.repo_service.event;

import com.codemind.repo_service.model.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepoIngestionEvent {
    private String repoId;
    private String userId;
    private String githubUrl;
    private String name;
    private String owner;
    private SourceType sourceType;
    private String githubToken;
}