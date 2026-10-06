package com.folkify.quiz.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.service.PlanPolicyTest;
import com.folkify.instrument.entity.Lesson;
import com.folkify.instrument.repository.LessonRepository;
import com.folkify.progress.dto.CompleteLessonResponse;
import com.folkify.progress.repository.UserLessonProgressRepository;
import com.folkify.progress.service.ProgressService;
import com.folkify.quiz.config.QuizProperties;
import com.folkify.quiz.dto.AdminQuizRequest;
import com.folkify.quiz.dto.QuizResponse;
import com.folkify.quiz.dto.QuizResultResponse;
import com.folkify.quiz.dto.SubmitQuizRequest;
import com.folkify.quiz.entity.QuestionType;
import com.folkify.quiz.entity.QuizOption;
import com.folkify.quiz.entity.QuizQuestion;
import com.folkify.quiz.repository.QuizAttemptRepository;
import com.folkify.quiz.repository.QuizQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    LessonRepository lessonRepo = mock(LessonRepository.class);
    QuizQuestionRepository questionRepo = mock(QuizQuestionRepository.class);
    QuizAttemptRepository attemptRepo = mock(QuizAttemptRepository.class);
    UserLessonProgressRepository progressRepo = mock(UserLessonProgressRepository.class);
    ProgressService progressService = mock(ProgressService.class);
    QuizService service = new QuizService(lessonRepo, questionRepo, attemptRepo, progressRepo,
            progressService, PlanPolicyTest.policy(), new QuizProperties());

    Lesson lesson = new Lesson();
    User user = PlanPolicyTest.user(Plan.BASIC, PlanPolicyTest.NOW.plusDays(5));
    QuizQuestion q1, q2, q3;

    static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    static QuizQuestion question(QuestionType type, boolean... correctFlags) {
        QuizQuestion q = withId(new QuizQuestion());
        q.setQuestion("q");
        q.setType(type);
        q.setExplanation("giai thich");
        for (int i = 0; i < correctFlags.length; i++) {
            q.addOption(withId(new QuizOption("o" + i, correctFlags[i], i)));
        }
        return q;
    }

    static UUID opt(QuizQuestion q, int index) {
        return q.getOptions().get(index).getId();
    }

    @BeforeEach
    void setUp() {
        withId(lesson);
        withId(user);
        lesson.setRequiredPlan(Plan.BASIC);
        q1 = question(QuestionType.SINGLE, true, false, false);
        q2 = question(QuestionType.SINGLE, false, true);
        q3 = question(QuestionType.MULTI, true, true, false);
        when(lessonRepo.findBySlugAndInstrumentSlug("dt-03", "dan-tranh")).thenReturn(Optional.of(lesson));
        when(questionRepo.findWithOptionsByLessonId(lesson.getId())).thenReturn(List.of(q1, q2, q3));
        when(progressService.completeLesson(any(), any())).thenReturn(CompleteLessonResponse.of(50, 50, 1, List.of()));
    }

    QuizResultResponse submit(Map<UUID, List<UUID>> answers) {
        return service.submit("dan-tranh", "dt-03", user, new SubmitQuizRequest(answers));
    }

    Map<UUID, List<UUID>> allCorrect() {
        return Map.of(
                q1.getId(), List.of(opt(q1, 0)),
                q2.getId(), List.of(opt(q2, 1)),
                q3.getId(), List.of(opt(q3, 0), opt(q3, 1)));
    }

    @Test
    void quizResponse_neverExposesCorrectness() {
        QuizResponse quiz = service.getQuiz("dan-tranh", "dt-03", user);
        assertThat(quiz.questions()).hasSize(3);
        assertThat(QuizResponse.OptionDto.class.getRecordComponents())
                .extracting(RecordComponent::getName).containsExactly("id", "text");
    }

    @Test
    void allCorrect_passesAndCompletesLessonOnce() {
        QuizResultResponse r = submit(allCorrect());
        assertThat(r.scorePercent()).isEqualTo(100);
        assertThat(r.passed()).isTrue();
        assertThat(r.completion()).isNotNull();
        verify(progressService, times(1)).completeLesson(lesson.getId(), user);
        verify(attemptRepo).save(any());
    }

    @Test
    void twoOfThree_failsWithoutCompletion() {
        QuizResultResponse r = submit(Map.of(
                q1.getId(), List.of(opt(q1, 0)),
                q2.getId(), List.of(opt(q2, 1))));
        assertThat(r.scorePercent()).isEqualTo(67);
        assertThat(r.passed()).isFalse();
        assertThat(r.completion()).isNull();
        verify(progressService, never()).completeLesson(any(), any());
        verify(attemptRepo).save(any());
    }

    @Test
    void multiWithPartialSelection_isWrong() {
        QuizResultResponse r = submit(Map.of(q3.getId(), List.of(opt(q3, 0))));
        assertThat(r.results().get(2).correct()).isFalse();
        assertThat(r.results().get(2).correctOptionIds()).containsExactly(opt(q3, 0), opt(q3, 1));
    }

    @Test
    void alreadyCompleted_doesNotCompleteAgain() {
        when(progressRepo.existsByIdUserIdAndIdLessonId(user.getId(), lesson.getId())).thenReturn(true);
        QuizResultResponse r = submit(allCorrect());
        assertThat(r.passed()).isTrue();
        assertThat(r.completion()).isNull();
        verify(progressService, never()).completeLesson(any(), any());
    }

    @Test
    void lockedLesson_throwsPlanRequired() {
        lesson.setRequiredPlan(Plan.PRO);
        assertThatThrownBy(() -> service.getQuiz("dan-tranh", "dt-03", user))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.PLAN_REQUIRED);
    }

    @Test
    void adminValidation_rejectsBadCorrectCounts() {
        var twoCorrect = List.of(new AdminQuizRequest.AdminOption("a", true), new AdminQuizRequest.AdminOption("b", true));
        assertThatThrownBy(() -> QuizService.validate(new AdminQuizRequest.AdminQuestion("q", QuestionType.SINGLE, null, twoCorrect)))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        var noneCorrect = List.of(new AdminQuizRequest.AdminOption("a", false), new AdminQuizRequest.AdminOption("b", false));
        assertThatThrownBy(() -> QuizService.validate(new AdminQuizRequest.AdminQuestion("q", QuestionType.MULTI, null, noneCorrect)))
                .isInstanceOf(ApiException.class);
    }
}
