package com.folkify.progress.service.impl;

import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.instrument.entity.Instrument;
import com.folkify.instrument.entity.Lesson;
import com.folkify.instrument.repository.InstrumentRepository;
import com.folkify.instrument.repository.LessonRepository;
import com.folkify.progress.dto.*;
import com.folkify.progress.entity.*;
import com.folkify.progress.repository.*;
import com.folkify.progress.service.ProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProgressServiceImpl implements ProgressService {

    private final UserLessonProgressRepository progressRepo;
    private final AchievementRepository achievementRepo;
    private final UserAchievementRepository userAchievementRepo;
    private final DailyActivityRepository dailyActivityRepo;
    private final LessonRepository lessonRepo;
    private final InstrumentRepository instrumentRepo;
    private final PlanPolicy planPolicy;

    public ProgressServiceImpl(
            UserLessonProgressRepository progressRepo,
            AchievementRepository achievementRepo,
            UserAchievementRepository userAchievementRepo,
            DailyActivityRepository dailyActivityRepo,
            LessonRepository lessonRepo,
            InstrumentRepository instrumentRepo,
            PlanPolicy planPolicy) {
        this.progressRepo = progressRepo;
        this.achievementRepo = achievementRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.dailyActivityRepo = dailyActivityRepo;
        this.lessonRepo = lessonRepo;
        this.instrumentRepo = instrumentRepo;
        this.planPolicy = planPolicy;
    }

    @Override
    @Transactional
    public CompleteLessonResponse completeLesson(UUID lessonId, User user) {
        if (progressRepo.existsByIdUserIdAndIdLessonId(user.getId(), lessonId)) {
            throw new ApiException(ErrorCode.LESSON_ALREADY_COMPLETED);
        }

        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.LESSON_NOT_FOUND));
        if (!planPolicy.canAccess(user, lesson.getRequiredPlan())) {
            throw new ApiException(ErrorCode.PLAN_REQUIRED);
        }

        progressRepo.save(new UserLessonProgress(user, lesson));

        LocalDate today = LocalDate.now();
        DailyActivity activity = dailyActivityRepo
                .findByIdUserIdAndIdActivityDate(user.getId(), today)
                .orElseGet(() -> new DailyActivity(user, today));
        activity.addXp(lesson.getXp());
        activity.addMinutes(15);
        dailyActivityRepo.save(activity);

        long totalXp = progressRepo.sumXpByUser(user.getId());
        long totalCompleted = progressRepo.countCompletedByUser(user.getId());
        List<LocalDate> activityDates = dailyActivityRepo.findAllActivityDatesByUserId(user.getId());
        int currentStreak = calculateCurrentStreak(activityDates);

        UUID instrumentId = lesson.getInstrument().getId();
        List<AchievementDto> newAchievements = checkAndUnlockAchievements(
                user, totalCompleted, totalXp, currentStreak, instrumentId);

        return CompleteLessonResponse.of(lesson.getXp(), totalXp, currentStreak, newAchievements);
    }

    @Override
    public UserProgressResponse getProgress(User user) {
        long totalXp = progressRepo.sumXpByUser(user.getId());
        int totalCompleted = (int) progressRepo.countCompletedByUser(user.getId());
        List<LocalDate> dates = dailyActivityRepo.findAllActivityDatesByUserId(user.getId());
        int currentStreak = calculateCurrentStreak(dates);
        int longestStreak = calculateLongestStreak(dates);

        List<Instrument> instruments = instrumentRepo.findAllByOrderByPopularityDesc();
        List<InstrumentProgressDto> instrumentProgress = instruments.stream()
                .map(inst -> {
                    int total = (int) lessonRepo.countByInstrumentId(inst.getId());
                    int completed = (int) progressRepo.countCompletedByUserAndInstrument(user.getId(), inst.getId());
                    return InstrumentProgressDto.of(inst, completed, total);
                })
                .toList();

        return UserProgressResponse.of(totalXp, totalCompleted, currentStreak, longestStreak, instrumentProgress);
    }

    @Override
    public AchievementsResponse getAchievements(User user) {
        List<UserAchievement> userAchievements = userAchievementRepo.findByUserIdWithAchievement(user.getId());
        Set<UUID> unlockedIds = userAchievements.stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        List<AchievementDto> unlocked = userAchievements.stream()
                .map(AchievementDto::fromUnlocked)
                .toList();

        List<AchievementDto> locked = achievementRepo.findAll().stream()
                .filter(a -> !unlockedIds.contains(a.getId()))
                .map(AchievementDto::fromLocked)
                .toList();

        return AchievementsResponse.of(unlocked, locked);
    }

    @Override
    public List<ActivityDayDto> getWeeklyActivity(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        List<DailyActivity> activities = dailyActivityRepo.findFromDateByUserId(user.getId(), weekStart);
        Map<LocalDate, DailyActivity> activityMap = activities.stream()
                .collect(Collectors.toMap(DailyActivity::getActivityDate, a -> a));

        List<ActivityDayDto> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DailyActivity da = activityMap.get(date);
            int xp = da != null ? da.getXpEarned() : 0;
            int minutes = da != null ? da.getMinutes() : 0;
            result.add(ActivityDayDto.of(date, xp, minutes, date.equals(today)));
        }
        return result;
    }

    private List<AchievementDto> checkAndUnlockAchievements(
            User user, long totalLessons, long totalXp, int streak, UUID instrumentId) {

        List<Achievement> all = achievementRepo.findAll();
        List<AchievementDto> newOnes = new ArrayList<>();

        for (Achievement achievement : all) {
            if (userAchievementRepo.existsByIdUserIdAndIdAchievementId(user.getId(), achievement.getId())) {
                continue;
            }

            boolean unlock = switch (achievement.getType()) {
                case LESSON -> totalLessons >= achievement.getThreshold();
                case XP -> totalXp >= achievement.getThreshold();
                case STREAK -> streak >= achievement.getThreshold();
                case INSTRUMENT -> {
                    long completedForInstrument = progressRepo.countCompletedByUserAndInstrument(user.getId(), instrumentId);
                    long totalForInstrument = lessonRepo.countByInstrumentId(instrumentId);
                    yield totalForInstrument > 0 && completedForInstrument >= totalForInstrument;
                }
            };

            if (unlock) {
                LocalDateTime now = LocalDateTime.now();
                userAchievementRepo.save(new UserAchievement(user, achievement));
                newOnes.add(AchievementDto.fromNewlyUnlocked(achievement, now));
            }
        }
        return newOnes;
    }

    private int calculateCurrentStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        LocalDate today = LocalDate.now();
        LocalDate expected = dates.contains(today) ? today : today.minusDays(1);
        if (!dates.contains(expected)) return 0;

        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    private int calculateLongestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        // dates are sorted DESC; reverse to ASC for iteration
        List<LocalDate> asc = new ArrayList<>(dates);
        Collections.reverse(asc);

        int longest = 1;
        int current = 1;
        for (int i = 1; i < asc.size(); i++) {
            if (asc.get(i).equals(asc.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}
