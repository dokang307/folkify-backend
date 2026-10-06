package com.folkify.practice.service;

import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.service.AiQuotaService;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.instrument.entity.Song;
import com.folkify.instrument.repository.SongRepository;
import com.folkify.practice.client.AiAnalysis;
import com.folkify.practice.client.AiServiceClient;
import com.folkify.practice.config.PracticeProperties;
import com.folkify.practice.dto.PerformanceResponse;
import com.folkify.practice.dto.PerformanceSummaryResponse;
import com.folkify.practice.entity.PerformanceAttempt;
import com.folkify.practice.repository.PerformanceAttemptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Luồng chấm điểm một tác phẩm: kiểm tra quyền/quota → gọi folkify_ai (ngoài transaction, có thể mất
 * vài chục giây với bản ghi dài) → trong 1 transaction: trừ quota + lưu kết quả. Lỗi phân tích không bị tính lượt.
 */
@Service
public class PerformanceService {

    private final SongRepository songRepository;
    private final PerformanceAttemptRepository attemptRepository;
    private final AiServiceClient aiServiceClient;
    private final AiQuotaService aiQuotaService;
    private final PlanPolicy planPolicy;
    private final PracticeProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public PerformanceService(SongRepository songRepository,
                              PerformanceAttemptRepository attemptRepository,
                              AiServiceClient aiServiceClient,
                              AiQuotaService aiQuotaService,
                              PlanPolicy planPolicy,
                              PracticeProperties properties,
                              TransactionTemplate transactionTemplate,
                              Clock clock) {
        this.songRepository = songRepository;
        this.attemptRepository = attemptRepository;
        this.aiServiceClient = aiServiceClient;
        this.aiQuotaService = aiQuotaService;
        this.planPolicy = planPolicy;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public PerformanceResponse submit(User user, UUID songId, MultipartFile file) {
        byte[] audio = readAudio(file);
        aiQuotaService.assertAvailable(user);

        Song song = songRepository.findWithInstrumentById(songId)
                .orElseThrow(() -> new ApiException(ErrorCode.SONG_NOT_FOUND));
        if (!planPolicy.canAccess(user, song.getRequiredPlan())) {
            throw new ApiException(ErrorCode.PLAN_REQUIRED);
        }
        if (!song.isScoringReady()) {
            throw new ApiException(ErrorCode.REFERENCE_NOT_READY);
        }

        AiAnalysis analysis = aiServiceClient.analyze(audio, file.getOriginalFilename(),
                song.getInstrument().getSlug(), song.getReferenceContour());

        PerformanceAttempt saved = transactionTemplate.execute(status -> {
            aiQuotaService.consume(user);
            return attemptRepository.save(new PerformanceAttempt(
                    user, song, analysis.overall(), analysis.pitchScore(), analysis.rhythmScore(),
                    analysis.stabilityScore(), analysis.raw(), analysis.feedback()));
        });
        return PerformanceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<PerformanceSummaryResponse> history(User user, int page, int size) {
        int pageSize = Math.max(1, Math.min(size, properties.getMaxPageSize()));
        var days = planPolicy.historyDays(planPolicy.effectivePlan(user));
        LocalDateTime after = days.isPresent()
                ? LocalDateTime.now(clock).minusDays(days.getAsInt())
                : LocalDateTime.of(1970, 1, 1, 0, 0);
        return attemptRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), after, PageRequest.of(Math.max(0, page), pageSize))
                .map(PerformanceSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PerformanceResponse get(User user, UUID id) {
        return attemptRepository.findByIdAndUserId(id, user.getId())
                .map(PerformanceResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private byte[] readAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.AUDIO_INVALID, "Chưa có file ghi âm");
        }
        if (file.getSize() > properties.getMaxUploadBytes()) {
            throw new ApiException(ErrorCode.AUDIO_INVALID, "File ghi âm vượt quá dung lượng cho phép");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.AUDIO_INVALID, "Không đọc được file ghi âm");
        }
    }
}
