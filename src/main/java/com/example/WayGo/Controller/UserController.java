package com.example.WayGo.Controller;

import com.example.WayGo.Dto.*;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Service.UserService;
import com.example.WayGo.Service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;

    /**
     * 아이디 중복 확인
     */
    @Operation(summary = "아이디 중복 확인", description = "회원가입 시 아이디 중복 여부를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 확인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/check-id")
    public ResponseEntity<Map<String, Object>> checkLoginId(
            @Parameter(description = "확인할 아이디", required = true, example = "testuser123")
            @RequestParam String loginId) {

        log.info("아이디 중복 확인 요청: {}", loginId);

        // 유효성 검사
        if (loginId == null || loginId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "아이디를 입력해주세요."
            ));
        }

        if (loginId.length() < 4 || loginId.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "아이디는 4자 이상 20자 이하로 입력해주세요."
            ));
        }

        if (!loginId.matches("^[a-zA-Z0-9]+$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "아이디는 영문자와 숫자만 사용 가능합니다."
            ));
        }

        boolean isDuplicate = userService.checkLoginIdDuplicate(loginId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("available", !isDuplicate);
        response.put("message", isDuplicate ? "이미 사용중인 아이디입니다." : "사용 가능한 아이디입니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 닉네임 중복 확인
     */
    @Operation(summary = "닉네임 중복 확인", description = "회원가입 시 닉네임 중복 여부를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 확인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(
            @Parameter(description = "확인할 닉네임", required = true, example = "홍길동")
            @RequestParam String nickname) {

        log.info("닉네임 중복 확인 요청: {}", nickname);

        // 유효성 검사
        if (nickname == null || nickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "닉네임을 입력해주세요."
            ));
        }

        if (nickname.length() < 2 || nickname.length() > 10) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "닉네임은 2자 이상 10자 이하로 입력해주세요."
            ));
        }

        boolean isDuplicate = userService.checkNicknameDuplicate(nickname);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("available", !isDuplicate);
        response.put("message", isDuplicate ? "이미 사용중인 닉네임입니다." : "사용 가능한 닉네임입니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 이메일 중복 확인
     */
    @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 여부를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "중복 확인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @Parameter(description = "확인할 이메일", required = true, example = "user@gmail.com")
            @RequestParam String email) {

        log.info("이메일 중복 확인 요청: {}", email);

        // 유효성 검사
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        // 이메일 형식 검증
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "올바른 이메일 형식이 아닙니다."
            ));
        }

        boolean isDuplicate = userService.checkEmailDuplicate(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("available", !isDuplicate);
        response.put("message", isDuplicate ? "이미 가입된 이메일입니다." : "사용 가능한 이메일입니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 이메일 인증번호 발송
     */
    @Operation(summary = "이메일 인증번호 발송", description = "회원가입을 위한 6자리 인증번호를 이메일로 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
            @ApiResponse(responseCode = "400", description = "발송 실패 또는 중복된 이메일"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/send-email-auth")
    public ResponseEntity<Map<String, Object>> sendEmailAuth(
            @Valid @RequestBody EmailRequest emailRequest,
            BindingResult bindingResult) {

        log.info("이메일 인증번호 발송 요청: {}", emailRequest.getEmail());

        // 유효성 검사 오류 처리
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("입력값을 확인해주세요.");

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", errorMessage
            ));
        }

        String email = emailRequest.getEmail();

        // 이메일 중복 확인
        if (userService.checkEmailDuplicate(email)) {
            log.warn("이미 가입된 이메일로 인증 시도: {}", email);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이미 가입된 이메일입니다."
            ));
        }

        // 이미 발송된 인증번호가 있는지 확인
        if (emailService.hasAuthCode(email)) {
            log.info("인증번호 재발송 방지: {}", email);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이미 인증번호가 발송되었습니다. 3분 후 다시 시도해주세요."
            ));
        }

        try {
            String authCode = emailService.sendAuthEmail(email);
            log.info("인증번호 발송 성공: {} -> {}", email, authCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증번호가 이메일로 발송되었습니다.");
            response.put("email", email);
            response.put("expiresIn", "3분");

            return ResponseEntity.ok(response);

        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
            ));
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "서버 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 이메일 인증번호 확인
     */
    @Operation(summary = "이메일 인증번호 확인", description = "발송된 6자리 인증번호를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @Valid @RequestBody EmailVerifyRequest verifyRequest,
            BindingResult bindingResult) {

        log.info("이메일 인증 확인 요청: {}", verifyRequest.getEmail());

        // 유효성 검사 오류 처리
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("입력값을 확인해주세요.");

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", errorMessage
            ));
        }

        boolean isValid = emailService.verifyAuthCode(
                verifyRequest.getEmail(),
                verifyRequest.getAuthCode()
        );

        Map<String, Object> response = new HashMap<>();

        if (isValid) {
            log.info("이메일 인증 성공: {}", verifyRequest.getEmail());
            response.put("success", true);
            response.put("verified", true);
            response.put("message", "이메일 인증이 완료되었습니다.");
            response.put("email", verifyRequest.getEmail());
            return ResponseEntity.ok(response);
        } else {
            log.warn("이메일 인증 실패: {}", verifyRequest.getEmail());
            response.put("success", false);
            response.put("verified", false);
            response.put("message", "인증번호가 일치하지 않거나 만료되었습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 회원가입
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 또는 중복")
    })
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(
            @Valid @RequestBody JoinRequest joinRequest,
            BindingResult bindingResult) {

        log.info("회원가입 요청: loginId={}, email={}", joinRequest.getLoginId(), joinRequest.getEmail());

        // 유효성 검사 오류 처리
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            FieldError::getDefaultMessage,
                            (existing, replacement) -> existing
                    ));

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "입력값을 확인해주세요.",
                    "errors", errors
            ));
        }

        // 비밀번호 일치 확인
        if (!joinRequest.getPassword().equals(joinRequest.getPasswordCheck())) {
            log.warn("비밀번호 불일치: {}", joinRequest.getLoginId());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "비밀번호가 일치하지 않습니다."
            ));
        }

        // ID 중복 체크
        if (userService.checkLoginIdDuplicate(joinRequest.getLoginId())) {
            log.warn("아이디 중복: {}", joinRequest.getLoginId());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이미 사용중인 아이디입니다."
            ));
        }

        // 닉네임 중복 체크
        if (userService.checkNicknameDuplicate(joinRequest.getNickname())) {
            log.warn("닉네임 중복: {}", joinRequest.getNickname());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이미 사용중인 닉네임입니다."
            ));
        }

        // 이메일 중복 체크
        if (userService.checkEmailDuplicate(joinRequest.getEmail())) {
            log.warn("이메일 중복: {}", joinRequest.getEmail());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이미 사용중인 이메일입니다."
            ));
        }

        // 이메일 인증 여부 확인 (선택적 - 필수로 만들려면 주석 해제)
        /*
        if (!Boolean.TRUE.equals(joinRequest.getEmailVerified())) {
            log.warn("이메일 미인증 회원가입 시도: {}", joinRequest.getEmail());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "이메일 인증을 완료해주세요."
            ));
        }
        */

        try {
            // 회원가입 진행
            userService.join(joinRequest);
            log.info("회원가입 성공: {}", joinRequest.getLoginId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("loginId", joinRequest.getLoginId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("회원가입 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "회원가입 처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 로그인
     */
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpSession session,
            BindingResult bindingResult) {

        log.info("로그인 시도: {}", loginRequest.getLoginId());

        // 유효성 검사 오류 처리
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "아이디와 비밀번호를 입력해주세요."
            ));
        }

        try {
            UserEntity user = userService.login(loginRequest);

            // 로그인 실패
            if (user == null) {
                log.warn("로그인 실패: {}", loginRequest.getLoginId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "아이디 또는 비밀번호가 일치하지 않습니다."
                ));
            }

            // 세션에 사용자 정보 저장
            session.setAttribute("userId", user.getId());
            session.setAttribute("loginId", user.getLoginId());
            session.setAttribute("userRole", user.getRole().toString());

            log.info("로그인 성공: {} (ID: {})", user.getLoginId(), user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인이 완료되었습니다.");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "loginId", user.getLoginId(),
                    "nickname", user.getNickname(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole().toString()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("로그인 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "로그인 처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 로그아웃합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        String loginId = (String) session.getAttribute("loginId");

        if (loginId != null) {
            log.info("로그아웃: {}", loginId);
        }

        // 세션 무효화
        session.invalidate();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃이 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Operation(summary = "사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        // 로그인 확인
        if (userId == null) {
            log.warn("미인증 사용자 정보 조회 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        try {
            UserEntity user = userService.getLoginUserById(userId);

            // 사용자 정보 없음
            if (user == null) {
                log.error("사용자 정보 없음: userId={}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "사용자 정보를 찾을 수 없습니다."
                ));
            }

            log.info("사용자 정보 조회: {} (ID: {})", user.getLoginId(), userId);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("success", true);
            userInfo.put("user", Map.of(
                    "id", user.getId(),
                    "loginId", user.getLoginId(),
                    "name", user.getName(),
                    "nickname", user.getNickname(),
                    "email", user.getEmail(),
                    "emailVerified", user.getEmailVerified(),
                    "role", user.getRole().toString()
            ));

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "사용자 정보 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 세션 확인 (로그인 상태 확인)
     */
    @Operation(summary = "세션 확인", description = "현재 로그인 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 확인 성공")
    })
    @GetMapping("/check-session")
    public ResponseEntity<Map<String, Object>> checkSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String loginId = (String) session.getAttribute("loginId");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isLoggedIn", userId != null);

        if (userId != null) {
            response.put("userId", userId);
            response.put("loginId", loginId);
            log.info("세션 확인: {} (ID: {})", loginId, userId);
        } else {
            log.info("세션 확인: 미로그인 상태");
        }

        return ResponseEntity.ok(response);
    }
}
