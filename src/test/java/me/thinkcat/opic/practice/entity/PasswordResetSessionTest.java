package me.thinkcat.opic.practice.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class PasswordResetSessionTest {

    private PasswordResetSession session;


    @BeforeEach
    void setup() {
        session = new PasswordResetSession();
        session.setCreatedAt();
    }

    @Test
    void 만료_시간_이내() {
        // when
        boolean flag = session.isSessionExpired(1);

        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 만료_시간_후() {
        // when
        boolean flag = session.isSessionExpired(0);

        // then
        Assertions.assertTrue(flag);
    }

    @Test
    void 어뷰징_의심_블록_시간_null() {
        // when
        boolean flag = session.isBlocked();

        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 어뷰징_의심_블록_시간_이내() {
        // given
        session.block(1);
        // when
        boolean flag = session.isBlocked();
        // then
        Assertions.assertTrue(flag);
    }

    @Test
    void 어뷰징_의심_블록_시간_경과_후() {
        // given
        session.block(0);
        // when
        boolean flag = session.isBlocked();
        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 전송_이전_쿨다운_시간_null() {
        // when
        boolean flag = session.isCoolDown(10);

        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 전송_이후_쿨다운_시간_이전() {
        // given
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        // when
        boolean flag = session.isCoolDown(10);
        // then
        Assertions.assertTrue(flag);
    }

    @Test
    void 전송_이후_쿨다운_시간_이후() {
        // given
        session.send("codeHash", LocalDateTime.now().plusMinutes(0));
        try { Thread.sleep(1); } catch (InterruptedException e) { }
        // when
        boolean flag = session.isCoolDown(0);
        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 코드_생성_전_만료_null() {
        // when
        boolean flag = session.isCodeExpired();
        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 코드_생성_후_만료_이내() {
        // given
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        // when
        boolean flag = session.isCodeExpired();
        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 코드_생성_후_만료_이후() {
        // given
        session.send("codeHash", LocalDateTime.now().plusMinutes(0));
        try { Thread.sleep(1); } catch (InterruptedException e) { }
        // when
        boolean flag = session.isCodeExpired();
        // then
        Assertions.assertTrue(flag);
    }

    @Test
    void 현재_코드로_최대_가능_시도_횟수_내() {
        String codeHash = "codeHash";
        String wrongCodeHash = "wrongCodeHash";
        // given
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        session.verify(wrongCodeHash);
        // when
        boolean flag = session.isExhausted(5);
        // then
        Assertions.assertFalse(flag);
    }

    @Test
    void 현재_코드로_최대_가능_시도_횟수_소진() {
        String codeHash = "codeHash";
        String wrongCodeHash = "wrongCodeHash";
        // given
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        session.verify(wrongCodeHash);
        session.verify(wrongCodeHash);
        session.verify(wrongCodeHash);
        // when
        boolean flag = session.isExhausted(1);
        // then
        Assertions.assertTrue(flag);
    }

    @Test
    void 올바른_해시_입력() {
        String codeHash = "codeHash";
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        // when
        boolean result = session.verify(codeHash);
        // then
        Assertions.assertTrue(result);
        Assertions.assertNotNull(session.getVerifiedAt());
    }

    @Test
    void 틀린_해시_입력() {
        String codeHash = "codeHash";
        String wrongCodeHash = "wrongCodeHash";
        // given
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        // when
        session.verify(wrongCodeHash);
        // then
        Assertions.assertNull(session.getVerifiedAt());
        Assertions.assertEquals(1, session.getAttemptCount());
    }

    @Test
    void 코드_재전송() {
        String codeHash = "codeHash";
        String wrongCodeHash = "wrongCodeHash";
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        session.verify(wrongCodeHash);
        // given
        String newCodeHash = "newCodeHash";
        // when
        session.send(newCodeHash, LocalDateTime.now().plusMinutes(5));
        // then
        Assertions.assertEquals(newCodeHash, session.getCodeHash());
        Assertions.assertEquals(0, session.getAttemptCount());
        Assertions.assertEquals(2, session.getResendCount());
    }

}
