package com.example.WayGo.Config.Itinerary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 *
 * RestTemplate이란?
 * - Spring에서 제공하는 HTTP 클라이언트
 * - 외부 API 호출할 때 사용
 * - Google Places API, Amadeus API 등
 *
 * 왜 @Bean으로 등록?
 * - Spring이 자동으로 생성/관리
 * - @Autowired로 주입 가능
 * - 싱글톤으로 재사용 (성능 최적화)
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate Bean 등록
     *
     * @Bean 어노테이션:
     * - 이 메서드가 반환하는 객체를 Spring Container에 등록
     * - 이름: "restTemplate"
     * - 타입: RestTemplate
     *
     * 이제 다른 클래스에서 사용 가능:
     * @Autowired
     * private RestTemplate restTemplate;
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}