package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.AbstractIntegrationTest;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class RetryFeedbackConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DrillAnswerService drillAnswerService;
    @Autowired private DrillAnswerRepository drillAnswerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LambdaAsyncClient lambdaAsyncClient;

    private Long drillAnswerId;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        Mockito.reset(lambdaAsyncClient);
        DrillAnswer answer = DrillAnswer.builder()
                .userId(userId).questionId(1L)
                .audioUrl("uploads/drills/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .build();
        answer.failFeedback();
        drillAnswerRepository.saveAndFlush(answer);
        drillAnswerId = answer.getId();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM drill_answers WHERE id = ?", drillAnswerId);
    }

    @Test
    void retryFeedback_동시_2회_요청시_Lambda는_1회만_invoke되고_상태는_1회만_전환된다() throws Exception {
        // given
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when: 동시 2회 retryFeedback 요청
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = List.of(
                executor.submit(() -> drillAnswerService.retryFeedback(drillAnswerId, userId)),
                executor.submit(() -> drillAnswerService.retryFeedback(drillAnswerId, userId))
        );

        List<Throwable> exceptions = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                exceptions.add(e.getCause());
            }
        }
        executor.shutdown();

        // then: 하나는 성공, 하나는 ValidationException
        Assertions.assertThat(exceptions).hasSize(1);
        Assertions.assertThat(exceptions.get(0)).isInstanceOf(ValidationException.class);

        // Lambda 1회만 invoke
        Mockito.verify(lambdaAsyncClient, Mockito.times(1)).invoke(ArgumentMatchers.any(InvokeRequest.class));

        // 최종 상태: REQUESTED_TRANSCRIPTION
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM drill_answers WHERE id = ?",
                String.class, drillAnswerId);
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION.getCode());
    }
}
