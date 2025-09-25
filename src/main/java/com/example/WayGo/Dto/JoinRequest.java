package com.example.WayGo.Dto;

import com.example.WayGo.Entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class JoinRequest {

    @NotBlank(message = "로그인 아이디가 비어있습니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 사용 가능합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호가 비어있습니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,16}$",
            message = "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인이 비어있습니다.")
    private String passwordCheck;

    @NotBlank(message = "닉네임이 비어있습니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
    private String nickname;

    @NotBlank(message = "이름이 비어있습니다.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
    private String name;

    @NotBlank(message = "이메일이 비어있습니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String emailAuthCode; // 이메일 인증번호

    private Boolean emailVerified = false; // 이메일 인증 여부

    public UserEntity toEntity(String encodedPassword) {
        return UserEntity.builder()
                .loginId(this.loginId)
                .password(encodedPassword)
                .nickname(this.nickname)
                .name(this.name)
                .email(this.email)
                .emailVerified(this.emailVerified)
                .build();
    }
}
