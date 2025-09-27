package com.project200.undabang.openchat.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.response.CreateOpenChatRoomResponse;
import com.project200.undabang.openchat.service.OpenChatRoomCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OpenChatRoomCommandController {

    private final OpenChatRoomCommandService openChatRoomCommandService;

    @PostMapping("/v1/open-chats")
    public ResponseEntity<CommonResponse<CreateOpenChatRoomResponse>> createOpenChatRoom(@Valid @RequestBody CreateOpenChatRoomRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(openChatRoomCommandService.createOpenChatRoom(request)));
    }
}
