package com.project200.undabang.common.web.advice;

import com.project200.undabang.common.web.response.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 REST 컨트롤러의 응답을 일관된 형식으로 래핑하는 글로벌 응답 래퍼입니다.
 * 이 클래스는 컨트롤러에서 반환하는 모든 응답을 HTTP STATUS OK에 {@link ApiResponse} 형식으로 자동 변환합니다.
 * {@link NoApiResponseWrapper} 어노테이션이 지정된 메소드나 클래스는 래핑 대상에서 제외됩니다.
 */
@RestControllerAdvice
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    /**
     * 특정 응답이 래핑 대상인지 결정합니다.
     *
     * @param returnType    컨트롤러 메소드의 반환 유형 정보
     * @param converterType 사용될 HTTP 메시지 컨버터 유형
     * @return 응답이 래핑되어야 하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // NoApiResponseWrapper 어노테이션이 메소드나 클래스에 있는지 확인
        if (returnType.getMethodAnnotation(NoApiResponseWrapper.class) != null ||
                (returnType.getContainingClass().isAnnotationPresent(NoApiResponseWrapper.class) &&
                        returnType.getMethodAnnotation(NoApiResponseWrapper.class) == null)) { // 클래스에만 있고 메소드에는 없을 때
            return false; // 래핑하지 않음
        }
        return true;
    }

    /**
     * 응답 본문(body)을 쓰기 전에 처리하여 필요한 경우 래핑합니다.
     *
     * @param body                  원본 응답 본문
     * @param returnType            컨트롤러 메소드의 반환 유형 정보
     * @param selectedContentType   선택된 응답 콘텐츠 유형
     * @param selectedConverterType 선택된 HTTP 메시지 컨버터 유형
     * @param request               서버 HTTP 요청 객체
     * @param response              서버 HTTP 응답 객체
     * @return 변환된 응답 본문 (래핑된 형태 또는 원본)
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 요청 경로 가져오기
        String path = request.getURI().getPath();

        // Spring Actuator 엔드포인트 응답은 래핑하지 않도록 처리 (경로 기반)
        if (path.startsWith("/actuator")) {
            return body;
        }

        // Swagger/OpenAPI 관련 경로도 래핑하지 않도록 처리
        // Swagger UI HTML 페이지, API docs (JSON/YAML) 등을 포함
        if (path.contains("swagger") || path.contains("api-docs") || path.contains("v3/api-docs") || path.contains("webjars")) {
            return body;
        }

        // body가 이미 ApiResponse 객체인 경우 (컨트롤러에서 직접 ApiResponse를 반환)
        if (body instanceof ApiResponse) {
            return body;
        }

        // 위와 중복이지만 재차 점검
        if (body instanceof ResponseEntity && ((ResponseEntity<?>) body).getBody() instanceof ApiResponse) {
            return body;
        }

        // body가 null이고, 메소드 반환 타입이 void가 아닌 경우 (예: Optional.empty() 반환 후 직렬화 결과가 null)
        // 또는 메소드 반환 타입이 void인 경우 (이때 body는 null)
        // 이 경우, 성공 응답으로 간주하고 data만 null인 ApiResponse를 생성합니다.
        if (body == null && returnType.getParameterType().equals(void.class)) {
            return ApiResponse.success("SUCCESS", "요청이 성공적으로 처리되었지만 반환할 데이터가 없습니다.");
        }

        // 원래 ResponseEntity의 상태 코드와 헤더를 유지하면서 본문만 ApiResponse로 변경된 형태로 반환
        return ApiResponse.success(body);
    }
}
