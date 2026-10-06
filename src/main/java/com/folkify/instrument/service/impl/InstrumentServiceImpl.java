package com.folkify.instrument.service.impl;

import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.instrument.dto.*;
import com.folkify.instrument.entity.Instrument;
import com.folkify.instrument.repository.InstrumentRepository;
import com.folkify.instrument.repository.LessonRepository;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.instrument.service.InstrumentService;
import com.folkify.progress.repository.UserLessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InstrumentServiceImpl implements InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final LessonRepository lessonRepository;
    private final PlanPolicy planPolicy;
    private final UserLessonProgressRepository progressRepository;

    public InstrumentServiceImpl(InstrumentRepository instrumentRepository,
                                 LessonRepository lessonRepository,
                                 PlanPolicy planPolicy,
                                 UserLessonProgressRepository progressRepository) {
        this.instrumentRepository = instrumentRepository;
        this.lessonRepository = lessonRepository;
        this.planPolicy = planPolicy;
        this.progressRepository = progressRepository;
    }

    @Override
    public List<InstrumentSummaryResponse> getAllInstruments() {
        return instrumentRepository.findAllByOrderByPopularityDesc()
                .stream()
                .map(InstrumentSummaryResponse::from)
                .toList();
    }

    @Override
    public InstrumentDetailResponse getInstrumentBySlug(String slug, User user) {
        Instrument instrument = instrumentRepository.findBySlugWithLessons(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.INSTRUMENT_NOT_FOUND));
        instrument.getFacts().size();
        instrument.getSongs().size();
        Set<UUID> completed = completedIds(user, slug);
        return InstrumentDetailResponse.from(instrument,
                l -> !planPolicy.canAccess(user, l.getRequiredPlan()),
                l -> completed.contains(l.getId()),
                s -> !planPolicy.canAccess(user, s.getRequiredPlan()));
    }

    @Override
    public List<LessonSummaryResponse> getLessonsByInstrument(String slug, User user) {
        if (!instrumentRepository.findBySlug(slug).isPresent()) {
            throw new ApiException(ErrorCode.INSTRUMENT_NOT_FOUND);
        }
        Set<UUID> completed = completedIds(user, slug);
        return lessonRepository.findByInstrumentSlugOrderByOrderIndexAsc(slug)
                .stream()
                .map(l -> LessonSummaryResponse.from(l, !planPolicy.canAccess(user, l.getRequiredPlan()), completed.contains(l.getId())))
                .toList();
    }

    @Override
    public LessonDetailResponse getLessonDetail(String instrumentSlug, String lessonSlug, User user) {
        var lesson = lessonRepository.findBySlugAndInstrumentSlug(lessonSlug, instrumentSlug)
                .orElseThrow(() -> new ApiException(ErrorCode.LESSON_NOT_FOUND));
        if (!planPolicy.canAccess(user, lesson.getRequiredPlan())) {
            throw new ApiException(ErrorCode.PLAN_REQUIRED);
        }
        // trigger lazy load của steps và tips trong transaction
        lesson.getSteps().size();
        lesson.getTips().size();
        return LessonDetailResponse.from(lesson);
    }

    @Override
    public List<SongResponse> getSongsByInstrument(String slug, User user) {
        Instrument instrument = instrumentRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.INSTRUMENT_NOT_FOUND));
        instrument.getSongs().size();
        return instrument.getSongs().stream()
                .map(s -> SongResponse.from(s, !planPolicy.canAccess(user, s.getRequiredPlan())))
                .toList();
    }

    private Set<UUID> completedIds(User user, String instrumentSlug) {
        return user == null ? Collections.emptySet() : progressRepository.findCompletedLessonIds(user.getId(), instrumentSlug);
    }
}
