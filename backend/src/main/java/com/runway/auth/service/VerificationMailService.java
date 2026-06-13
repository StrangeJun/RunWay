package com.runway.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.delivery-mode:smtp}")
    private String deliveryMode;

    @Value("${app.mail.from:no-reply@runway.run}")
    private String from;

    public void sendVerificationCode(String email, String code, boolean passwordReset) {
        String subject = passwordReset ? "[RunWay] 비밀번호 재설정 인증번호" : "[RunWay] 이메일 인증번호";
        String body = "인증번호는 " + code + " 입니다.\n10분 안에 입력해 주세요.\n본인이 요청하지 않았다면 이 메일을 무시하세요.";

        if ("log".equalsIgnoreCase(deliveryMode)) {
            log.info("LOCAL ONLY verification mail: email={}, subject={}, code={}", email, subject, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
