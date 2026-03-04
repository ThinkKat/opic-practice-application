package me.thinkcat.opic.practice.controller;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.response.NoticeResponse;
import me.thinkcat.opic.practice.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<NoticeResponse>>> getNotices() {
        List<NoticeResponse> notices = noticeService.getNotices();
        return ResponseEntity.ok(
                CommonResponse.<List<NoticeResponse>>builder()
                        .success(true)
                        .result(notices)
                        .message("Notices retrieved successfully")
                        .build()
        );
    }
}
