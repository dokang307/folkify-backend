package com.folkify.practice.controller;

import com.folkify.admin.dto.SongAdminResponse;
import com.folkify.common.response.ApiResponse;
import com.folkify.practice.service.SongReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/songs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Song Reference", description = "Bản mẫu để AI chấm điểm tác phẩm")
public class AdminSongReferenceController {

    private final SongReferenceService songReferenceService;

    public AdminSongReferenceController(SongReferenceService songReferenceService) {
        this.songReferenceService = songReferenceService;
    }

    @PostMapping(value = "/{id}/reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload audio mẫu của tác phẩm → phân tích đường cao độ → tác phẩm chấm điểm được")
    public ResponseEntity<ApiResponse<SongAdminResponse>> uploadReference(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(songReferenceService.uploadReference(id, file)));
    }
}
