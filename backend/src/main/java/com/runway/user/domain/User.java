package com.runway.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @JsonIgnore
    @Column(name = "refresh_token_hash", length = 512)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "social_provider", length = 20)
    private String socialProvider;

    @Column(name = "social_id", length = 255)
    private String socialId;

    @Builder
    private User(String email, String passwordHash, String nickname,
                 String profileImageUrl, String bio,
                 String socialProvider, String socialId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.socialProvider = socialProvider;
        this.socialId = socialId;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String nickname, String profileImageUrl, String bio) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
    }

    public void updateRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public void linkSocial(String provider, String id) {
        this.socialProvider = provider;
        this.socialId = id;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
