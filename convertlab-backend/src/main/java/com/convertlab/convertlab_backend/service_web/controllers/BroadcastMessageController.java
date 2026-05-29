package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.BroadcastMessageService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.BroadcastMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/broadcasts")
@RequiredArgsConstructor
public class BroadcastMessageController {

    private final BroadcastMessageService broadcastMessageService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BroadcastMessageResponse>>> getActiveBroadcasts() {
        return ResponseEntity.ok(ApiResponse.success(broadcastMessageService.getActiveBroadcasts()));
    }
}
