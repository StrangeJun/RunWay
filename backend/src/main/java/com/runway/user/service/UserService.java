package com.runway.user.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.user.domain.User;
import com.runway.user.dto.UpdateProfileRequest;
import com.runway.user.dto.UserProfileResponse;
import com.runway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findActiveUser(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);

        String newNickname = request.getNickname() != null ? request.getNickname() : user.getNickname();
        String newProfileImageUrl = request.getProfileImageUrl() != null ? request.getProfileImageUrl() : user.getProfileImageUrl();
        String newBio = request.getBio() != null ? request.getBio() : user.getBio();

        // 닉네임이 변경된 경우에만 중복 확인
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNicknameAndIdNot(request.getNickname(), userId)) {
                throw new RunwayException(ErrorCode.DUPLICATED_NICKNAME);
            }
        }

        user.updateProfile(newNickname, newProfileImageUrl, newBio);
        log.info("User profile updated: {}", user.getEmail());
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findActiveUser(userId);
        user.softDelete();
        user.updateRefreshTokenHash(null);
        log.info("User soft deleted: {}", user.getEmail());
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.USER_NOT_FOUND));
    }
}
