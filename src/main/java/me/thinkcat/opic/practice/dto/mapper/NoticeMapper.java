package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.NoticeResponse;
import me.thinkcat.opic.practice.entity.Notice;

public class NoticeMapper {

    private NoticeMapper() {}

    public static NoticeResponse toResponse(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
