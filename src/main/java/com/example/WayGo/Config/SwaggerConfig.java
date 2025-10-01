package com.example.WayGo.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WayGo API") // 제목 변경
                        .version("v1.0.0")
                        .description("여행 커뮤니티 API - 사용자 관리, 게시글, 좋아요 기능")); // 설명 업데이트
    }
}
