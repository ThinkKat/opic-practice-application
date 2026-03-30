package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lambda invoke 실패 시 feedbackStatus를 FAILED로 변경하는 서비스.
 * FeedbackLambdaService의 whenComplete 콜백에서만 호출된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LambdaFeedbackStatusUpdater {

    private final AnswerRepository answerRepository;
    private final DrillAnswerRepository drillAnswerRepository;

    @Transactional
    public void failSessionFeedback(String audioUrl) {
        Answer answer = answerRepository.findByAudioUrl(audioUrl).orElse(null);
        if (answer == null) {
            log.warn("event=lambda_invoke_fail_answer_not_found | audioUrl={}", audioUrl);
            return;
        }
        answer.failFeedback();
        answerRepository.save(answer);
        log.error("event=lambda_invoke_fail_feedback_failed | answerId={} | audioUrl={}",
                answer.getId(), audioUrl);
    }

    @Transactional
    public void failDrillFeedback(String audioUrl) {
        DrillAnswer answer = drillAnswerRepository.findByAudioUrl(audioUrl).orElse(null);
        if (answer == null) {
            log.warn("event=lambda_invoke_fail_drill_answer_not_found | audioUrl={}", audioUrl);
            return;
        }
        answer.failFeedback();
        drillAnswerRepository.save(answer);
        log.error("event=lambda_invoke_fail_drill_feedback_failed | answerId={} | audioUrl={}",
                answer.getId(), audioUrl);
    }
}
