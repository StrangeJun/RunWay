package com.runway.user.service;

import com.runway.attempt.domain.enums.CourseAttemptStatus;
import com.runway.attempt.repository.CourseAttemptRepository;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.course.repository.CourseRepository;
import com.runway.run.domain.enums.RunningRecordStatus;
import com.runway.run.repository.RunningRecordRepository;
import com.runway.run.repository.RunSummaryProjection;
import com.runway.user.domain.User;
import com.runway.user.dto.AchievementItemResponse;
import com.runway.user.dto.AchievementsResponse;
import com.runway.user.dto.UpdateProfileRequest;
import com.runway.user.dto.UserProfileResponse;
import com.runway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final CourseAttemptRepository courseAttemptRepository;
    private final CourseRepository courseRepository;

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
            if (userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(request.getNickname(), userId)) {
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

    @Transactional(readOnly = true)
    public AchievementsResponse getAchievements(UUID userId) {
        RunningRecordStatus completed = RunningRecordStatus.COMPLETED;

        // DB aggregate 쿼리로 기본 집계
        long completedRuns = runningRecordRepository.countByUserIdAndStatus(userId, completed);
        double totalDistanceMeters = runningRecordRepository.sumDistanceMetersByUserIdAndStatus(userId, completed);

        long completedAttempts = courseAttemptRepository.countByUserIdAndStatus(userId, CourseAttemptStatus.COMPLETED);
        long createdCourses = courseRepository.countByCreatorIdAndDeletedAtIsNull(userId);

        // 경량 summary projection으로 streak 및 unlock 날짜 계산
        List<RunSummaryProjection> runSummaries = runningRecordRepository
                .findRunSummariesByUserIdAndStatus(userId, completed);

        // streak 계산
        int longestStreak = computeLongestStreak(runSummaries);

        List<AchievementItemResponse> items = new ArrayList<>();

        // FIRST_RUN
        Instant firstRunAt = runSummaries.isEmpty() ? null : runSummaries.get(0).getStartedAt();
        items.add(AchievementItemResponse.builder()
                .code("FIRST_RUN")
                .title("첫 러닝")
                .description("첫 러닝을 완료했습니다.")
                .unlocked(completedRuns >= 1)
                .unlockedAt(completedRuns >= 1 ? firstRunAt : null)
                .progress(Math.min(completedRuns, 1))
                .target(1)
                .build());

        // TOTAL_10K / 50K / 100K
        items.add(buildDistanceAchievement("TOTAL_10K", "누적 10km", "총 10km를 달성했습니다.",
                10_000, totalDistanceMeters, runSummaries));
        items.add(buildDistanceAchievement("TOTAL_50K", "누적 50km", "총 50km를 달성했습니다.",
                50_000, totalDistanceMeters, runSummaries));
        items.add(buildDistanceAchievement("TOTAL_100K", "누적 100km", "총 100km를 달성했습니다.",
                100_000, totalDistanceMeters, runSummaries));

        // STREAK_3 / STREAK_7
        items.add(buildStreakAchievement("STREAK_3", "3일 연속 러닝", "3일 연속으로 러닝을 완료했습니다.",
                3, longestStreak));
        items.add(buildStreakAchievement("STREAK_7", "7일 연속 러닝", "7일 연속으로 러닝을 완료했습니다.",
                7, longestStreak));

        // FIRST_COURSE_CREATED
        Instant firstCourseAt = courseRepository.findFirstByCreatorIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId)
                .map(c -> c.getCreatedAt()).orElse(null);
        items.add(AchievementItemResponse.builder()
                .code("FIRST_COURSE_CREATED")
                .title("첫 코스 등록")
                .description("첫 번째 코스를 등록했습니다.")
                .unlocked(createdCourses >= 1)
                .unlockedAt(createdCourses >= 1 ? firstCourseAt : null)
                .progress(Math.min(createdCourses, 1))
                .target(1)
                .build());

        // FIRST_COURSE_COMPLETED
        Instant firstAttemptAt = courseAttemptRepository
                .findFirstByUserIdAndStatusOrderByCompletedAtAsc(userId, CourseAttemptStatus.COMPLETED)
                .map(a -> a.getCompletedAt()).orElse(null);
        items.add(AchievementItemResponse.builder()
                .code("FIRST_COURSE_COMPLETED")
                .title("첫 코스 완주")
                .description("첫 번째 코스를 완주했습니다.")
                .unlocked(completedAttempts >= 1)
                .unlockedAt(completedAttempts >= 1 ? firstAttemptAt : null)
                .progress(Math.min(completedAttempts, 1))
                .target(1)
                .build());

        return AchievementsResponse.builder().items(items).build();
    }

    private AchievementItemResponse buildDistanceAchievement(
            String code, String title, String description,
            long targetMeters, double totalDistance, List<RunSummaryProjection> runs) {

        boolean unlocked = totalDistance >= targetMeters;
        Instant unlockedAt = null;
        if (unlocked) {
            // 누적 거리가 threshold를 넘은 시점의 런 날짜를 계산
            double acc = 0;
            for (RunSummaryProjection r : runs) {
                acc += r.getDistanceMeters() != null ? r.getDistanceMeters() : 0.0;
                if (acc >= targetMeters) {
                    unlockedAt = r.getStartedAt();
                    break;
                }
            }
        }
        return AchievementItemResponse.builder()
                .code(code)
                .title(title)
                .description(description)
                .unlocked(unlocked)
                .unlockedAt(unlockedAt)
                .progress((long) Math.min(totalDistance, targetMeters))
                .target(targetMeters)
                .build();
    }

    private int computeLongestStreak(List<RunSummaryProjection> runs) {
        if (runs.isEmpty()) return 0;
        Set<LocalDate> dates = new HashSet<>();
        for (RunSummaryProjection r : runs) {
            dates.add(r.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate());
        }
        List<LocalDate> sorted = new ArrayList<>(dates);
        sorted.sort(null);
        int longest = 1, cur = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                cur++;
                if (cur > longest) longest = cur;
            } else {
                cur = 1;
            }
        }
        return longest;
    }

    private AchievementItemResponse buildStreakAchievement(
            String code, String title, String description, int target, int longestStreak) {
        return AchievementItemResponse.builder()
                .code(code)
                .title(title)
                .description(description)
                .unlocked(longestStreak >= target)
                .unlockedAt(null)
                .progress(Math.min(longestStreak, target))
                .target(target)
                .build();
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.USER_NOT_FOUND));
    }
}
