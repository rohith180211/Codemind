package com.codemind.repo_service.repository;

import com.codemind.repo_service.model.Repository;
import com.codemind.repo_service.model.RepoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@org.springframework.stereotype.Repository
public interface RepoRepository extends JpaRepository<Repository, UUID> {

    List<Repository> findByUserId(UUID userId);

    Optional<Repository> findByUserIdAndId(UUID userId, UUID repoId);

    boolean existsByUserIdAndGithubUrl(UUID userId, String githubUrl);

    List<Repository> findByUserIdAndStatus(UUID userId, RepoStatus status);
}