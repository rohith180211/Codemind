package com.codemind.repo_service.service;

import com.codemind.repo_service.event.RepoIngestionEvent;
import com.codemind.repo_service.kafka.RepoEventProducer;
import com.codemind.repo_service.model.Repository;
import com.codemind.repo_service.model.RepoStatus;
import com.codemind.repo_service.model.SourceType;
import com.codemind.repo_service.repository.RepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final RepoRepository repoRepository;
    private final RepoEventProducer repoEventProducer;


    public Repository addRepo(UUID userId, String githubUrl,
                              SourceType sourceType, String githubToken) {

        if (repoRepository.existsByUserIdAndGithubUrl(userId, githubUrl)) {
            throw new RuntimeException("Repository already added");
        }

        String[] parsed = parseGithubUrl(githubUrl);
        String owner = parsed[0];
        String name = parsed[1];

        Repository repo = Repository.builder()
                .userId(userId)
                .githubUrl(githubUrl)
                .name(name)
                .owner(owner)
                .sourceType(sourceType)
                .status(RepoStatus.PENDING)
                .build();

        Repository saved = repoRepository.save(repo);

        RepoIngestionEvent event = RepoIngestionEvent.builder()
                .repoId(saved.getId().toString())
                .userId(userId.toString())
                .githubUrl(githubUrl)
                .name(name)
                .owner(owner)
                .sourceType(sourceType)
                .githubToken(githubToken)
                .build();

        repoEventProducer.publishIngestionEvent(event);

        return saved;
    }


    public List<Repository> getUserRepos(UUID userId) {
        return repoRepository.findByUserId(userId);
    }

    public Optional<Repository> getRepo(UUID userId, UUID repoId) {
        return repoRepository.findByUserIdAndId(userId, repoId);
    }

    public void deleteRepo(UUID userId, UUID repoId) {
        Repository repo = repoRepository.findByUserIdAndId(userId, repoId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
        repoRepository.delete(repo);
    }

    public Repository updateStatus(UUID repoId, RepoStatus status, String errorMessage) {
        Repository repo = repoRepository.findById(repoId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
        repo.setStatus(status);
        repo.setErrorMessage(errorMessage);
        return repoRepository.save(repo);
    }

    private String[] parseGithubUrl(String url) {
        // handles https://github.com/owner/repo and https://github.com/owner/repo.git
        try {
            String cleaned = url.replace(".git", "");
            String[] parts = cleaned.split("/");
            String owner = parts[parts.length - 2];
            String name = parts[parts.length - 1];
            return new String[]{owner, name};
        } catch (Exception e) {
            throw new RuntimeException("Invalid GitHub URL: " + url);
        }
    }
}