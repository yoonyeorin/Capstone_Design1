package com.example.WayGo.Service;

import com.example.WayGo.Dto.ChangeNicknameRequest;
import com.example.WayGo.Dto.ChangePasswordRequest;
import com.example.WayGo.Dto.JoinRequest;
import com.example.WayGo.Dto.LoginRequest;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    /**
     * 아이디 중복 체크
     */
    public boolean checkLoginIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    /**
     * 닉네임 중복 체크
     */
    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 이메일 중복 체크
     */
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 회원가입
     */
    public void join(JoinRequest req) {
        UserEntity user = req.toEntity(encoder.encode(req.getPassword()));
        userRepository.save(user);
        log.info("회원가입 완료: {}", req.getLoginId());
    }
    /**
     * 프로필 이미지 업데이트
     */
    public void updateProfileImage(Long userId, String imageUrl) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.updateProfileImage(imageUrl);
        userRepository.save(user);
        log.info("프로필 이미지 업데이트: userId={}, url={}", userId, imageUrl);
    }

    /**
     * 배경 이미지 업데이트
     */
    public void updateBackgroundImage(Long userId, String imageUrl) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.updateBackgroundImage(imageUrl);
        userRepository.save(user);
        log.info("배경 이미지 업데이트: userId={}, url={}", userId, imageUrl);
    }

    /**
     * 로그인
     */
    public UserEntity login(LoginRequest req) {
        Optional<UserEntity> optionalUser = userRepository.findByLoginId(req.getLoginId());

        if(optionalUser.isEmpty()) {
            return null;
        }

        UserEntity user = optionalUser.get();

        if(!encoder.matches(req.getPassword(), user.getPassword())) {
            return null;
        }

        log.info("로그인 성공: {}", req.getLoginId());
        return user;
    }

    /**
     * ID로 사용자 조회
     */
    public UserEntity getLoginUserById(Long userId) {
        if(userId == null) return null;

        Optional<UserEntity> optionalUser = userRepository.findById(userId);
        return optionalUser.orElse(null);
    }

    /**
     * 로그인 ID로 사용자 조회
     */
    public UserEntity getLoginUserByLoginId(String loginId) {
        if(loginId == null) return null;

        Optional<UserEntity> optionalUser = userRepository.findByLoginId(loginId);
        return optionalUser.orElse(null);
    }

    /**
     * 비밀번호 변경
     */
    public boolean changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!encoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("비밀번호 변경 실패 - 현재 비밀번호 불일치: userId={}", userId);
            return false;
        }

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getNewPasswordCheck())) {
            log.warn("비밀번호 변경 실패 - 새 비밀번호 불일치: userId={}", userId);
            return false;
        }

        // 새 비밀번호 암호화 후 저장
        String encodedNewPassword = encoder.encode(request.getNewPassword());
        user.updatePassword(encodedNewPassword);
        userRepository.save(user);

        log.info("비밀번호 변경 성공: userId={}", userId);
        return true;
    }

    /**
     * 닉네임 변경
     */
    public boolean changeNickname(Long userId, ChangeNicknameRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 닉네임과 동일한지 확인
        if (user.getNickname().equals(request.getNickname())) {
            log.warn("닉네임 변경 실패 - 기존과 동일한 닉네임: userId={}, nickname={}", userId, request.getNickname());
            return false;
        }

        // 닉네임 중복 확인
        if (checkNicknameDuplicate(request.getNickname())) {
            log.warn("닉네임 변경 실패 - 중복된 닉네임: userId={}, nickname={}", userId, request.getNickname());
            return false;
        }

        // 닉네임 업데이트
        user.updateNickname(request.getNickname());
        userRepository.save(user);

        log.info("닉네임 변경 성공: userId={}, oldNickname={}, newNickname={}",
                userId, user.getNickname(), request.getNickname());
        return true;
    }
}

