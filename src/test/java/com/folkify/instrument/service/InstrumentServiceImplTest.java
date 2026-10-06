package com.folkify.instrument.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.entitlement.service.PlanPolicyTest;
import com.folkify.instrument.dto.LessonSummaryResponse;
import com.folkify.instrument.entity.Instrument;
import com.folkify.instrument.entity.Lesson;
import com.folkify.instrument.repository.InstrumentRepository;
import com.folkify.instrument.repository.LessonRepository;
import com.folkify.instrument.service.impl.InstrumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class InstrumentServiceImplTest {

    InstrumentRepository instrumentRepo = Mockito.mock(InstrumentRepository.class);
    LessonRepository lessonRepo = Mockito.mock(LessonRepository.class);
    PlanPolicy planPolicy = PlanPolicyTest.policy();
    InstrumentServiceImpl service = new InstrumentServiceImpl(instrumentRepo, lessonRepo, planPolicy,
            Mockito.mock(com.folkify.progress.repository.UserLessonProgressRepository.class));

    Lesson free = lesson("dt-01", Plan.FREE);
    Lesson basic = lesson("dt-03", Plan.BASIC);
    Lesson pro = lesson("dt-09", Plan.PRO);

    static Lesson lesson(String slug, Plan plan) {
        Lesson l = new Lesson();
        l.setSlug(slug);
        l.setTitle(slug);
        l.setRequiredPlan(plan);
        l.setYoutubeVideoId("vid-" + slug);
        return l;
    }

    @BeforeEach
    void setUp() {
        when(instrumentRepo.findBySlug("dan-tranh")).thenReturn(Optional.of(new Instrument()));
        when(lessonRepo.findByInstrumentSlugOrderByOrderIndexAsc("dan-tranh")).thenReturn(List.of(free, basic, pro));
        when(lessonRepo.findBySlugAndInstrumentSlug("dt-03", "dan-tranh")).thenReturn(Optional.of(basic));
        when(lessonRepo.findBySlugAndInstrumentSlug("dt-09", "dan-tranh")).thenReturn(Optional.of(pro));
    }

    @Test
    void anonymousList_marksPaidLessonsLocked() {
        List<LessonSummaryResponse> list = service.getLessonsByInstrument("dan-tranh", null);
        assertThat(list).extracting(LessonSummaryResponse::locked).containsExactly(false, true, true);
        assertThat(list).extracting(LessonSummaryResponse::requiredPlan).containsExactly(Plan.FREE, Plan.BASIC, Plan.PRO);
    }

    @Test
    void basicUserList_unlocksBasicOnly() {
        User basicUser = PlanPolicyTest.user(Plan.BASIC, PlanPolicyTest.NOW.plusDays(5));
        assertThat(service.getLessonsByInstrument("dan-tranh", basicUser))
                .extracting(LessonSummaryResponse::locked).containsExactly(false, false, true);
    }

    @Test
    void anonymousDetailOnBasicLesson_throwsPlanRequired() {
        assertThatThrownBy(() -> service.getLessonDetail("dan-tranh", "dt-03", null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PLAN_REQUIRED);
    }

    @Test
    void expiredProUser_isTreatedAsFree() {
        User expired = PlanPolicyTest.user(Plan.PRO, PlanPolicyTest.NOW.minusDays(1));
        assertThatThrownBy(() -> service.getLessonDetail("dan-tranh", "dt-09", expired))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void proUserDetail_returnsVideo() {
        User proUser = PlanPolicyTest.user(Plan.PRO, PlanPolicyTest.NOW.plusDays(5));
        assertThat(service.getLessonDetail("dan-tranh", "dt-09", proUser).youtubeVideoId()).isEqualTo("vid-dt-09");
    }
}
