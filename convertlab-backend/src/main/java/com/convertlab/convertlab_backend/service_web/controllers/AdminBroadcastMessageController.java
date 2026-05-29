package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.BroadcastMessageService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.BroadcastMessageResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.CreateBroadcastRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/broadcasts")
@RequiredArgsConstructor
public class AdminBroadcastMessageController {

    private final BroadcastMessageService broadcastMessageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BroadcastMessageResponse>>> getBroadcasts() {
        return ResponseEntity.ok(ApiResponse.success(broadcastMessageService.getAdminBroadcasts()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BroadcastMessageResponse>> createBroadcast(
            @RequestBody CreateBroadcastRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(broadcastMessageService.createBroadcast(request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<BroadcastMessageResponse>> deactivateBroadcast(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(broadcastMessageService.deactivateBroadcast(id)));
    }
}
