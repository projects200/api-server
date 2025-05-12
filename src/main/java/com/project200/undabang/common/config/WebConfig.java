package com.project200.undabang.common.config;

import com.project200.undabang.common.web.interceptor.XUserEmailCheckInterceptor;
import com.project200.undabang.common.web.interceptor.XUserIdCheckInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
        registry.addInterceptor(xUserIdCheckInterceptor())
                .excludePathPatterns(
                        "/docs/**",
                        "/generated-docs/**",
                        "/api-documentation/**",
                        "/webjars/**",
                        "/build/**",
                        "/reports/**",
                        "/favicon.ico"
                )
                .addPathPatterns("/**");

        registry.addInterceptor(xUserEmailCheckInterceptor()).addPathPatterns("/auth/**");
    }

    /**
     * 정적 리소스를 처리하기 위한 핸들러를 추가합니다.
     * 이 메서드는 특정 경로에 대해 리소스의 위치와 캐시 설정을 정의합니다.
     *
     * @param registry 리소스 핸들러를 등록하기 위한 ResourceHandlerRegistry 객체
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/build/reports/**")
                .addResourceLocations("classpath:/static/", "file:./build/reports/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }
}
