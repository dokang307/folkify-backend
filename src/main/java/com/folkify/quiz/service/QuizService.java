package com.folkify.quiz.service;

import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.instrument.entity.Lesson;
import com.folkify.instrument.repository.LessonRepository;
import com.folkify.progress.dto.CompleteLessonResponse;
import com.folkify.progress.repository.UserLessonProgressRepository;
import com.folkify.progress.service.ProgressService;
import com.folkify.quiz.config.QuizProperties;
import com.folkify.quiz.dto.*;
import com.folkify.quiz.entity.QuestionType;
import com.folkify.quiz.entity.QuizAttempt;
import com.folkify.quiz.entity.QuizOption;
import com.folkify.quiz.entity.QuizQuestion;
import com.folkify.quiz.repository.QuizAttemptRepository;
import com.folkify.quiz.repository.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class QuizService {

    private final LessonRepository lessonRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserLessonProgressRepository progressRepository;
    private final ProgressService progressService;
    private final PlanPolicy planPolicy;
    private final QuizProperties properties;

    public QuizService(LessonRepository lessonRepository,
                       QuizQuestionRepository questionRepository,
                       QuizAttemptRepository attemptRepository,
                       UserLessonProgressRepository progressRepository,
                       ProgressService progressService,
                       PlanPolicy planPolicy,
                       QuizProperties properties) {
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.progressService = progressService;
        this.planPolicy = planPolicy;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(String instrumentSlug, String lessonSlug, User user) {
        Lesson lesson = accessibleLesson(instrumentSlug, lessonSlug, user);
        List<QuizQuestion> questions = requireQuestions(lesson);
        return new QuizResponse(lesson.getId(), properties.getPassPercent(),
                questions.stream().map(QuizResponse.QuestionDto::from).toList());
    }

    @Transactional
    public QuizResultResponse submit(String instrumentSlug, String lessonSlug, User user, SubmitQuizRequest request) {
        Lesson lesson = accessibleLesson(instrumentSlug, lessonSlug, user);
        List<QuizQuestion> questions = requireQuestions(lesson);
        Map<UUID, List<UUID>> answers = request.answers() != null ? request.answers() : Map.of();

        List<QuizResultResponse.QuestionResult> results = new ArrayList<>();
        int correctCount = 0;
        for (QuizQuestion q : questions) {
            Set<UUID> correctIds = new LinkedHashSet<>();
            q.getOptions().stream().filter(QuizOption::isCorrect).forEach(o -> correctIds.add(o.getId()));
            Set<UUID> chosen = new HashSet<>(answers.getOrDefault(q.getId(), List.of()));
            boolean correct = !chosen.isEmpty() && chosen.equals(correctIds);
            if (correct) correctCount++;
            results.add(new QuizResultResponse.QuestionResult(q.getId(), correct, List.copyOf(correctIds), q.getExplanation()));
        }

        int total = questions.size();
        int scorePercent = Math.round(100f * correctCount / total);
        boolean passed = scorePercent >= properties.getPassPercent();
        attemptRepository.save(new QuizAttempt(user, lesson, scorePercent, correctCount, total, answers));

        CompleteLessonResponse completion = null;
        if (passed && !progressRepository.existsByIdUserIdAndIdLessonId(user.getId(), lesson.getId())) {
            completion = progressService.completeLesson(lesson.getId(), user);
        }
        return new QuizResultResponse(scorePercent, correctCount, total, passed, results, completion);
    }

    @Transactional(readOnly = true)
    public AdminQuizResponse getAdminQuiz(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.LESSON_NOT_FOUND));
        return new AdminQuizResponse(lesson.getId(),
                questionRepository.findWithOptionsByLessonId(lesson.getId()).stream()
                        .map(AdminQuizResponse.Question::from).toList());
    }

    @Transactional
    public AdminQuizResponse replaceQuiz(UUID lessonId, AdminQuizRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.LESSON_NOT_FOUND));
        request.questions().forEach(QuizService::validate);

        questionRepository.deleteByLessonId(lessonId);
        questionRepository.flush();

        List<QuizQuestion> saved = new ArrayList<>();
        for (int i = 0; i < request.questions().size(); i++) {
            AdminQuizRequest.AdminQuestion aq = request.questions().get(i);
            QuizQuestion q = new QuizQuestion();
            q.setLesson(lesson);
            q.setQuestion(aq.question());
            q.setType(aq.type());
            q.setExplanation(aq.explanation());
            q.setOrderIndex(i);
            for (int j = 0; j < aq.options().size(); j++) {
                AdminQuizRequest.AdminOption ao = aq.options().get(j);
                q.addOption(new QuizOption(ao.text(), ao.correct(), j));
            }
            saved.add(questionRepository.save(q));
        }
        return new AdminQuizResponse(lessonId, saved.stream().map(AdminQuizResponse.Question::from).toList());
    }

    static void validate(AdminQuizRequest.AdminQuestion q) {
        long correct = q.options().stream().filter(AdminQuizRequest.AdminOption::correct).count();
        if (q.type() == QuestionType.SINGLE && correct != 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Câu hỏi một đáp án phải có đúng 1 đáp án đúng: " + q.question());
        }
        if (q.type() == QuestionType.MULTI && correct < 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Câu hỏi nhiều đáp án phải có ít nhất 1 đáp án đúng: " + q.question());
        }
    }

    private Lesson accessibleLesson(String instrumentSlug, String lessonSlug, User user) {
        Lesson lesson = lessonRepository.findBySlugAndInstrumentSlug(lessonSlug, instrumentSlug)
                .orElseThrow(() -> new ApiException(ErrorCode.LESSON_NOT_FOUND));
        if (!planPolicy.canAccess(user, lesson.getRequiredPlan())) {
            throw new ApiException(ErrorCode.PLAN_REQUIRED);
        }
        return lesson;
    }

    private List<QuizQuestion> requireQuestions(Lesson lesson) {
        List<QuizQuestion> questions = questionRepository.findWithOptionsByLessonId(lesson.getId());
        if (questions.isEmpty()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Bài học này chưa có câu hỏi ôn tập");
        }
        return questions;
    }
}
