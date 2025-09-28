package com.example.WayGo.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "현재 비밀번호가 비어있습니다.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호가 비어있습니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,16}$",
            message = "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인이 비어있습니다.")
    private String newPasswordCheck;
}
