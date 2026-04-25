package com.codemind.repo_service.controller;

import com.codemind.repo_service.dto.AddRepoRequest;
import com.codemind.repo_service.dto.RepoResponse;
import com.codemind.repo_service.model.Repository;
import com.codemind.repo_service.service.JwtService;
import com.codemind.repo_service.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<?> addRepo(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AddRepoRequest request) {
        try {
            UUID userId = extractUserId(authHeader);
            Repository repo = repoService.addRepo(
                    userId,
                    request.getGithubUrl(),
                    request.getSourceType(),
                    request.getGithubToken()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserRepos(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        List<RepoResponse> repos = repoService.getUserRepos(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(repos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRepo(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        UUID userId = extractUserId(authHeader);
        return repoService.getRepo(userId, id)
                .map(repo -> ResponseEntity.ok().body(toResponse(repo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRepo(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        try {
            UUID userId = extractUserId(authHeader);
            repoService.deleteRepo(userId, id);
            return ResponseEntity.ok(Map.of("message", "Repository removed"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getRepoStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        UUID userId = extractUserId(authHeader);
        return repoService.getRepo(userId, id)
                .map(repo -> ResponseEntity.ok().body(Map.of(
                        "id", repo.getId(),
                        "status", repo.getStatus(),
                        "name", repo.getName()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        String userIdStr = jwtService.extractUserId(token);
        return UUID.fromString(userIdStr);
    }

    private RepoResponse toResponse(Repository repo) {
        return RepoResponse.builder()
                .id(repo.getId())
                .githubUrl(repo.getGithubUrl())
                .name(repo.getName())
                .owner(repo.getOwner())
                .sourceType(repo.getSourceType())
                .status(repo.getStatus())
                .createdAt(repo.getCreatedAt())
                .build();
    }
}