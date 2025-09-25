package com.example.WayGo.Entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean emailVerified;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @PrePersist
    public void prePersist() {
        this.role = this.role == null ? UserRole.USER : this.role;
        this.emailVerified = this.emailVerified == null ? false : this.emailVerified;
    }

    // 이메일 인증 완료 메서드
    public void verifyEmail() {
        this.emailVerified = true;
    }
}
