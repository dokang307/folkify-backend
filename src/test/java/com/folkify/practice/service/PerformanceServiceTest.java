package com.folkify.practice.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.service.AiQuotaService;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.entitlement.service.PlanPolicyTest;
import com.folkify.instrument.entity.Instrument;
import com.folkify.instrument.entity.Song;
import com.folkify.instrument.repository.SongRepository;
import com.folkify.practice.client.AiAnalysis;
import com.folkify.practice.client.AiServiceClient;
import com.folkify.practice.config.PracticeProperties;
import com.folkify.practice.dto.PerformanceResponse;
import com.folkify.practice.entity.PerformanceAttempt;
import com.folkify.practice.repository.PerformanceAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PerformanceServiceTest {

    SongRepository songRepo = mock(SongRepository.class);
    PerformanceAttemptRepository attemptRepo = mock(PerformanceAttemptRepository.class);
    AiServiceClient ai = mock(AiServiceClient.class);
    AiQuotaService quota = mock(AiQuotaService.class);
    PlanPolicy planPolicy = PlanPolicyTest.policy();
    TransactionTemplate tx = mock(TransactionTemplate.class);
    PerformanceService service = new PerformanceService(songRepo, attemptRepo, ai, quota, planPolicy,
            new PracticeProperties(), tx, PlanPolicyTest.CLOCK);

    Instrument danBau = new Instrument();
    User basic = PlanPolicyTest.user(Plan.BASIC, PlanPolicyTest.NOW.plusDays(5));
    MockMultipartFile take = new MockMultipartFile("file", "take.webm", "audio/webm", new byte[]{1, 2, 3});
    Map<String, Object> contour = Map.of("times", List.of(0.0), "midi", List.of(67.0), "durationSeconds", 180.0);
    AiAnalysis analysis = new AiAnalysis(88, 90, 85, 92, List.of("Khá tốt"), Map.of("overall", 88, "coveragePercent", 30));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReflectionTestUtils.setField(danBau, "slug", "dan-bau");
        ReflectionTestUtils.setField(basic, "id", UUID.randomUUID());
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
        when(attemptRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    Song song(Plan plan, boolean ready) {
        Song s = new Song();
        ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
        s.setInstrument(danBau);
        s.setTitle("Trống cơm");
        s.setRequiredPlan(plan);
        if (ready) {
            s.attachReference("https://cdn/trong-com.mp3", contour, 180.0);
        }
        when(songRepo.findWithInstrumentById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void scoresAgainstSongReference_andConsumesQuotaOnce() {
        Song s = song(Plan.BASIC, true);
        when(ai.analyze(any(), any(), eq("dan-bau"), eq(contour))).thenReturn(analysis);
        PerformanceResponse r = service.submit(basic, s.getId(), take);
        assertThat(r.songTitle()).isEqualTo("Trống cơm");
        assertThat(r.overall()).isEqualTo(88);
        assertThat(r.feedback()).containsExactly("Khá tốt");
        verify(quota).assertAvailable(basic);
        verify(quota, times(1)).consume(basic);
    }

    @Test
    void unknownSong_isNotFound() {
        assertThatThrownBy(() -> service.submit(basic, UUID.randomUUID(), take))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
        verifyNoInteractions(ai);
    }

    @Test
    void quotaCheckFailure_neverCallsAi() {
        Song s = song(Plan.BASIC, true);
        doThrow(new ApiException(ErrorCode.AI_NOT_IN_PLAN)).when(quota).assertAvailable(basic);
        assertThatThrownBy(() -> service.submit(basic, s.getId(), take))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.AI_NOT_IN_PLAN);
        verifyNoInteractions(ai);
    }

    @Test
    void aiFailure_doesNotConsumeQuota() {
        Song s = song(Plan.BASIC, true);
        when(ai.analyze(any(), any(), any(), any())).thenThrow(new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> service.submit(basic, s.getId(), take)).isInstanceOf(ApiException.class);
        verify(quota, never()).consume(any());
        verify(attemptRepo, never()).save(any(PerformanceAttempt.class));
    }

    @Test
    void songAbovePlan_requiresUpgrade() {
        Song s = song(Plan.PRO, true);
        assertThatThrownBy(() -> service.submit(basic, s.getId(), take))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.PLAN_REQUIRED);
        verifyNoInteractions(ai);
    }

    @Test
    void songWithoutReference_isNotReady() {
        Song s = song(Plan.BASIC, false);
        assertThatThrownBy(() -> service.submit(basic, s.getId(), take))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.REFERENCE_NOT_READY);
        verifyNoInteractions(ai);
    }

    @Test
    void emptyFile_isInvalidAudio() {
        Song s = song(Plan.BASIC, true);
        MockMultipartFile empty = new MockMultipartFile("file", "x.webm", "audio/webm", new byte[0]);
        assertThatThrownBy(() -> service.submit(basic, s.getId(), empty))
                .isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(ErrorCode.AUDIO_INVALID);
    }
}
