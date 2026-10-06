package com.folkify;

import com.folkify.auth.entity.User;
import com.folkify.auth.repository.UserRepository;
import com.folkify.practice.client.AiAnalysis;
import com.folkify.practice.client.AiServiceClient;
import com.folkify.practice.service.PerformanceService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Khởi động toàn bộ app trên Postgres nhúng: chạy hết migration Flyway và Hibernate
 * {@code ddl-auto: validate} — bắt lệch giữa entity và schema mà unit test không thấy.
 */
@SpringBootTest(properties = {
        "r2.account-id=test", "r2.access-key-id=test", "r2.secret-access-key=test",
        "r2.bucket-name=test", "r2.public-url=http://localhost/r2" // scalability-ok: dummy test value
})
@AutoConfigureMockMvc
class PersistenceIntegrationTest {

    static final EmbeddedPostgres POSTGRES = start();

    static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Không khởi động được Postgres nhúng", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stop() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PerformanceService performanceService;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    AiServiceClient aiServiceClient;

    /**
     * Chạy luồng chấm điểm theo bài mẫu với entity thật (ngoài transaction, open-in-view tắt):
     * bắt lỗi LazyInitializationException mà unit test có repo giả không thấy được.
     */
    @Test
    void songSubmit_loadsSongInstrument_andConsumesQuotaOnce() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, password, role, plan, plan_expires_at) "
                + "VALUES (?, 'Test', ?, 'x', 'USER', 'BASIC', NOW() + INTERVAL '10 days')", userId, userId + "@test.vn");
        UUID songId = UUID.randomUUID();
        jdbc.update("INSERT INTO songs (id, instrument_id, title, order_index, required_plan, reference_contour, reference_duration_seconds) "
                + "SELECT ?, id, 'Trống cơm', 99, 'BASIC', CAST(? AS jsonb), 180 FROM instruments WHERE slug = 'dan-bau'",
                songId, "{\"times\":[0.0],\"midi\":[67.0],\"durationSeconds\":180}");
        when(aiServiceClient.analyze(any(), any(), eq("dan-bau"), any()))
                .thenReturn(new AiAnalysis(88, 90, 85, 92, List.of("Khá tốt"), Map.of("overall", 88, "coveragePercent", 30)));

        User user = userRepository.findById(userId).orElseThrow();
        var result = performanceService.submit(user, songId,
                new MockMultipartFile("file", "take.webm", "audio/webm", new byte[]{1, 2, 3}));

        assertThat(result.instrumentSlug()).isEqualTo("dan-bau");
        assertThat(result.songTitle()).isEqualTo("Trống cơm");
        assertThat(jdbc.queryForObject("SELECT used_count FROM user_ai_usage WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void songList_exposesScoringReadiness_andHidesReferenceWhenLocked() throws Exception {
        jdbc.update("UPDATE songs SET required_plan = 'PRO', reference_audio_url = 'https://cdn/x.mp3', "
                + "reference_contour = CAST('{\"times\":[0],\"midi\":[60]}' AS jsonb) "
                + "WHERE instrument_id = (SELECT id FROM instruments WHERE slug = 'sao-truc') AND order_index = 0");
        mockMvc.perform(get("/api/instruments/sao-truc/songs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].scoringReady").value(true))
                .andExpect(jsonPath("$.result[0].locked").value(true))
                .andExpect(jsonPath("$.result[0].requiredPlan").value("PRO"))
                .andExpect(jsonPath("$.result[0].referenceAudioUrl").doesNotExist());
    }

    @Test
    void migrationsApply_andGatingSeededFromOrderIndex() {
        Integer version = jdbc.queryForObject(
                "SELECT max(CAST(version AS INTEGER)) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(18);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM lessons WHERE order_index < 2 AND required_plan <> 'FREE'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM lessons WHERE order_index >= 2 AND required_plan = 'FREE'", Integer.class)).isZero();
    }

    @Test
    void youtubeLessonSeed_coversFiveInstrumentsWithQuizzes() {
        Integer instrumentsWithPath = jdbc.queryForObject(
                "SELECT count(*) FROM (SELECT i.slug FROM lessons l JOIN instruments i ON i.id = l.instrument_id "
                        + "WHERE l.youtube_video_id IS NOT NULL "
                        + "AND i.slug IN ('dan-tranh','dan-bau','dan-nhi','dan-ty-ba','sao-truc') "
                        + "GROUP BY i.slug HAVING count(*) >= 6) t", Integer.class);
        assertThat(instrumentsWithPath).isEqualTo(5);
        Integer lessonsWithoutQuiz = jdbc.queryForObject(
                "SELECT count(*) FROM lessons l WHERE l.youtube_video_id IS NOT NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM quiz_questions q WHERE q.lesson_id = l.id)", Integer.class);
        assertThat(lessonsWithoutQuiz).isZero();
        Integer singleWithWrongCorrectCount = jdbc.queryForObject(
                "SELECT count(*) FROM quiz_questions q WHERE q.type = 'SINGLE' AND "
                        + "(SELECT count(*) FROM quiz_options o WHERE o.question_id = q.id AND o.is_correct) <> 1", Integer.class);
        assertThat(singleWithWrongCorrectCount).isZero();
    }

    @Test
    void anonymousLessonList_exposesLockState() throws Exception {
        mockMvc.perform(get("/api/instruments/dan-tranh/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].locked").value(false))
                .andExpect(jsonPath("$.result[0].requiredPlan").value("FREE"))
                .andExpect(jsonPath("$.result[0].youtubeUrl").doesNotExist());
    }

    @Test
    void anonymousLockedLessonDetail_returnsPlanRequired() throws Exception {
        String lockedSlug = jdbc.queryForObject(
                "SELECT l.slug FROM lessons l JOIN instruments i ON i.id = l.instrument_id "
                        + "WHERE i.slug = 'dan-tranh' AND l.required_plan <> 'FREE' ORDER BY l.order_index LIMIT 1",
                String.class);
        mockMvc.perform(get("/api/instruments/dan-tranh/lessons/" + lockedSlug))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1106));
    }
}
