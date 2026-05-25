package com.i_route.backend.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.base-url}")
    private String baseUrl;

    // 이메일 인증 링크 발송
    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/api/auth/email/verify?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[i-Route] 이메일 인증을 완료해주세요");
            helper.setText(
                    "<h2>i-Route 이메일 인증</h2>" +
                            "<p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>" +
                            "<a href='" + verifyUrl + "' style='padding:10px 20px; background:#4A90E2; color:white; " +
                            "text-decoration:none; border-radius:5px;'>이메일 인증하기</a>" +
                            "<p>링크는 1시간 후 만료됩니다.</p>",
                    true // HTML 여부
            );

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    // 환영 이메일 발송
    public void sendWelcomeEmail(String toEmail, String nickname) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[i-Route] 가입을 환영합니다!");
            helper.setText(
                    "<h2>" + nickname + "님, 환영합니다! 🎉</h2>" +
                            "<p>i-Route 서비스 가입을 축하드립니다.</p>",
                    true
            );

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("환영 이메일 발송에 실패했습니다.");
        }
    }
}
