package com.runway.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationMailService {

    private static final String BANNER_CONTENT_ID = "runway-banner";
    private static final String BANNER_PATH = "mail/runway-email-banner.png";

    private final JavaMailSender mailSender;

    @Value("${app.mail.delivery-mode:smtp}")
    private String deliveryMode;

    @Value("${app.mail.from:no-reply@runway.run}")
    private String from;

    public void sendVerificationCode(String email, String code, boolean passwordReset) {
        String subject = passwordReset ? "[RunWay] 비밀번호 재설정 인증번호" : "[RunWay] 이메일 인증번호";
        if ("log".equalsIgnoreCase(deliveryMode)) {
            log.info("LOCAL ONLY verification mail: email={}, subject={}, code={}", email, subject, code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(buildHtml(code, passwordReset), true);
            helper.addInline(
                    BANNER_CONTENT_ID,
                    new ClassPathResource(BANNER_PATH),
                    "image/png"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", email, e);
            throw new IllegalStateException("인증 메일을 발송하지 못했습니다.", e);
        }
    }

    private String buildHtml(String code, boolean passwordReset) {
        String heading = passwordReset ? "비밀번호를 재설정하시나요?" : "RunWay 가입을 환영합니다";
        String introduction = passwordReset
                ? "아래 인증번호를 입력하면 새 비밀번호를 설정할 수 있습니다."
                : "아래 인증번호를 입력해 이메일 인증을 완료해 주세요.";

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                </head>
                <body style="margin:0;padding:0;background:#080c0b;font-family:Arial,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;color:#f4f7f5;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#080c0b;padding:32px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#0b100e;border:1px solid #334329;border-radius:22px;overflow:hidden;">
                          <tr>
                            <td>
                              <img src="cid:%s" alt="RunWay" width="620" style="display:block;width:100%%;height:auto;border:0;">
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:34px 38px 12px;">
                              <p style="margin:0 0 10px;color:#b8ff00;font-size:13px;font-weight:700;letter-spacing:1.5px;">EXPLORE · RUN · SHARE</p>
                              <h1 style="margin:0 0 14px;font-size:26px;line-height:1.35;color:#ffffff;">%s</h1>
                              <p style="margin:0;color:#b7c0bb;font-size:15px;line-height:1.75;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 38px;">
                              <div style="background:#111914;border:1px solid #5f7c2e;border-radius:16px;padding:24px;text-align:center;">
                                <p style="margin:0 0 10px;color:#8d9992;font-size:12px;font-weight:700;letter-spacing:1.5px;">VERIFICATION CODE</p>
                                <p style="margin:0;color:#b8ff00;font-size:38px;font-weight:800;letter-spacing:10px;">%s</p>
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:6px 38px 36px;">
                              <p style="margin:0 0 8px;color:#dce2de;font-size:14px;line-height:1.7;">인증번호는 <strong style="color:#ffffff;">10분 동안</strong> 유효합니다.</p>
                              <p style="margin:0;color:#77817b;font-size:12px;line-height:1.7;">본인이 요청하지 않았다면 이 메일을 무시해 주세요. 인증번호는 다른 사람에게 알려주지 마세요.</p>
                            </td>
                          </tr>
                        </table>
                        <p style="margin:18px 0 0;color:#59625d;font-size:11px;">© RunWay. Keep moving forward.</p>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(BANNER_CONTENT_ID, heading, introduction, code);
    }
}
