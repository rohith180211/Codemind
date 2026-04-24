package com.codemind.repo_service.dto;

import com.codemind.repo_service.model.SourceType;
import lombok.Data;

@Data
public class AddRepoRequest {
    private String githubUrl;
    private SourceType sourceType;
}