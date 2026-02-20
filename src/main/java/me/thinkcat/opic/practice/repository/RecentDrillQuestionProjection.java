package me.thinkcat.opic.practice.repository;

import java.time.LocalDateTime;

public interface RecentDrillQuestionProjection {
    Long getQuestionId();
    LocalDateTime getLastPracticedAt();
    Long getDrillPracticeCount();
}
