package com.project200.undabang.common.config;

import com.project200.undabang.common.web.interceptor.XUserEmailCheckInterceptor;
import com.project200.undabang.common.web.interceptor.XUserIdCheckInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정 클래스입니다.
 * 이 클래스는 인터셉터 등록 및 기타 웹 관련 설정을 담당합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * XUserIdCheckInterceptor 빈을 생성합니다.
     * 이 인터셉터는 요청에 X-User-Id 헤더가 있는지 확인합니다.
     *
     * @return XUserIdCheckInterceptor 인스턴스
     */
    @Bean
    public XUserIdCheckInterceptor xUserIdCheckInterceptor() {
        return new XUserIdCheckInterceptor();
    }

    @Bean
    public XUserEmailCheckInterceptor xUserEmailCheckInterceptor(){
        return new XUserEmailCheckInterceptor();
    }


    /**
     * 애플리케이션에서 사용할 인터셉터를 등록합니다.
     * XUserIdCheckInterceptor가 모든 경로 패턴에 적용됩니다.
     *
     * @param registry 인터셉터 레지스트리
     */

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(xUserIdCheckInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(xUserEmailCheckInterceptor()).addPathPatterns("/auth/**");
    }
}
