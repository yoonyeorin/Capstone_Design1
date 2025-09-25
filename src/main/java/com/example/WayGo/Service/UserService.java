package com.example.WayGo.Service;

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
}

