package com.folkify.practice.service;

import com.folkify.admin.dto.SongAdminResponse;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.instrument.entity.Song;
import com.folkify.instrument.repository.SongRepository;
import com.folkify.practice.client.AiServiceClient;
import com.folkify.practice.config.PracticeProperties;
import com.folkify.storage.service.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Gắn audio mẫu cho tác phẩm: phân tích đường cao độ bằng folkify_ai rồi lưu file lên R2. */
@Service
public class SongReferenceService {

    private static final String REFERENCE_FOLDER = "song-references";

    private final SongRepository songRepository;
    private final StorageService storageService;
    private final AiServiceClient aiServiceClient;
    private final PracticeProperties properties;

    public SongReferenceService(SongRepository songRepository, StorageService storageService,
                                AiServiceClient aiServiceClient, PracticeProperties properties) {
        this.songRepository = songRepository;
        this.storageService = storageService;
        this.aiServiceClient = aiServiceClient;
        this.properties = properties;
    }

    @Transactional
    public SongAdminResponse uploadReference(UUID songId, MultipartFile file) {
        Song song = songRepository.findWithInstrumentById(songId)
                .orElseThrow(() -> new ApiException(ErrorCode.SONG_NOT_FOUND));
        if (file == null || file.isEmpty() || file.getSize() > properties.getMaxUploadBytes()) {
            throw new ApiException(ErrorCode.AUDIO_INVALID, "File bản mẫu trống hoặc quá lớn");
        }
        byte[] audio;
        try {
            audio = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.AUDIO_INVALID, "Không đọc được file bản mẫu");
        }
        // Phân tích trước: audio hỏng thì không tốn chỗ lưu trữ
        Map<String, Object> contour = aiServiceClient.extractReference(
                audio, file.getOriginalFilename(), song.getInstrument().getSlug());
        Object duration = contour.get("durationSeconds");
        String url = storageService.upload(file, REFERENCE_FOLDER);
        song.attachReference(url, contour, duration instanceof Number n ? n.doubleValue() : null);
        return SongAdminResponse.from(song);
    }
}
