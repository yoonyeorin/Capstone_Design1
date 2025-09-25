package com.example.WayGo.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 인증번호를 메모리에 임시 저장 (Redis 대신 사용)
    private final ConcurrentHashMap<String, String> authCodeStorage = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 이메일로 인증번호 발송
     */
    public String sendAuthEmail(String toEmail) throws MessagingException {
        // 6자리 인증번호 생성
        String authCode = createAuthCode();

        // 이메일 발송
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("[회원가입] 이메일 인증번호");

        String emailContent = createEmailContent(authCode);
        helper.setText(emailContent, true);

        javaMailSender.send(message);

        // 인증번호를 메모리에 저장 (3분 후 자동 삭제)
        saveAuthCode(toEmail, authCode);

        log.info("인증번호 발송 완료: {}", toEmail);
        return authCode;
    }

    /**
     * 6자리 랜덤 인증번호 생성
     */
    private String createAuthCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    /**
     * 이메일 내용 생성
     */
    private String createEmailContent(String authCode) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<style>");
        content.append("body { font-family: 'Noto Sans KR', sans-serif; background-color: #f5f5f5; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".card { background-color: white; border-radius: 10px; padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        content.append(".header { text-align: center; margin-bottom: 30px; }");
        content.append(".title { color: #333; font-size: 24px; font-weight: bold; margin-bottom: 10px; }");
        content.append(".auth-code-box { background-color: #f8f9fa; border: 2px solid #007bff; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }");
        content.append(".auth-code { font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px; }");
        content.append(".info { color: #666; font-size: 14px; line-height: 1.6; }");
        content.append(".warning { color: #dc3545; font-size: 13px; margin-top: 20px; }");
        content.append(".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef; color: #999; font-size: 12px; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='container'>");
        content.append("<div class='card'>");
        content.append("<div class='header'>");
        content.append("<h1 class='title'>이메일 인증</h1>");
        content.append("</div>");
        content.append("<p class='info'>안녕하세요.<br>회원가입을 위한 이메일 인증번호를 안내해드립니다.</p>");
        content.append("<div class='auth-code-box'>");
        content.append("<div class='auth-code'>" + authCode + "</div>");
        content.append("</div>");
        content.append("<p class='info'>위 인증번호를 회원가입 페이지에 입력해주세요.</p>");
        content.append("<p class='warning'>※ 본 인증번호는 3분간 유효합니다.<br>※ 본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.</p>");
        content.append("<div class='footer'>");
        content.append("<p>본 메일은 발신 전용이며, 회신되지 않습니다.</p>");
        content.append("</div>");
        content.append("</div>");
        content.append("</div>");
        content.append("</body>");
        content.append("</html>");

        return content.toString();
    }

    /**
     * 인증번호 저장 (3분 후 자동 삭제)
     */
    private void saveAuthCode(String email, String authCode) {
        authCodeStorage.put(email, authCode);

        // 3분 후 자동 삭제
        scheduler.schedule(() -> {
            authCodeStorage.remove(email);
            log.info("인증번호 만료: {}", email);
        }, 3, TimeUnit.MINUTES);
    }

    /**
     * 인증번호 확인
     */
    public boolean verifyAuthCode(String email, String authCode) {
        String savedCode = authCodeStorage.get(email);

        if (savedCode != null && savedCode.equals(authCode)) {
            authCodeStorage.remove(email); // 인증 성공 시 삭제
            return true;
        }

        return false;
    }

    /**
     * 인증번호 존재 확인
     */
    public boolean hasAuthCode(String email) {
        return authCodeStorage.containsKey(email);
    }
}

