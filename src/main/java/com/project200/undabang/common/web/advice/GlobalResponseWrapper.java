package com.project200.undabang.common.web.advice;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.common.web.response.SuccessDetails;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 REST 컨트롤러의 응답을 일관된 형식으로 래핑하는 글로벌 응답 래퍼입니다.
 * 이 클래스는 컨트롤러에서 반환하는 모든 응답을 HTTP STATUS OK에 {@link CommonResponse} 형식으로 자동 변환합니다.
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
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // NoApiResponseWrapper 어노테이션이 메소드나 클래스에 있는지 확인
        // 클래스에만 있고 메소드에는 없을 때
        return returnType.getMethodAnnotation(NoApiResponseWrapper.class) == null &&
                (!returnType.getContainingClass().isAnnotationPresent(NoApiResponseWrapper.class) ||
                        returnType.getMethodAnnotation(NoApiResponseWrapper.class) != null); // 래핑하지 않음
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
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, @NonNull ServerHttpResponse response) {

        // 요청 경로 가져오기
        String path = request.getURI().getPath();
        if (isExcludedPath(path)) {
            return body;
        }

        // body가 이미 CommonResponse 객체인 경우 (예: 예외 처리기에서 직접 CommonResponse 반환)
        if (body instanceof CommonResponse) {
            return body;
        }

        // ResponseEntity를 처리하는 경우: 상태 코드와 헤더를 유지하고, 본문만 래핑합니다.
        if (body instanceof ResponseEntity) {
            return handleResponseEntity(body);
        }

        // 메소드 반환 타입이 void인 경우 (이때 body는 null)
        // 이 경우, 성공 응답으로 간주하고 data만 null인 특정 메시지를 포함한 CommonResponse를 생성합니다.
        if (isVoidReturnType(body, returnType)) {
            return handleVoidReturnType();
        }

        // 그 외의 경우, 본문을 CommonResponse.success로 래핑합니다.
        return CommonResponse.success(body);
    }

    // ResponseEntity를 처리하는 메소드
    // ResponseEntity의 본문이 CommonResponse인 경우, 그대로 반환합니다.
    private Object handleResponseEntity(Object responseBody) {
        ResponseEntity<?> responseEntity = (ResponseEntity<?>) responseBody;
        Object originalResponseEntityBody = responseEntity.getBody();

        // ResponseEntity의 본문이 이미 CommonResponse인 경우, 그대로 반환합니다.
        if (originalResponseEntityBody instanceof CommonResponse) {
            return responseEntity;
        }

        // ResponseEntity의 본문을 CommonResponse로 래핑합니다.
        // null 본문도 CommonResponse.success(null)로 처리됩니다.
        CommonResponse<?> wrappedBody = CommonResponse.success(originalResponseEntityBody);
        return new ResponseEntity<>(wrappedBody, responseEntity.getHeaders(), responseEntity.getStatusCode());
    }

    // 메소드 반환 타입이 void인 경우, 성공 응답으로 간주하고 data만 null인 특정 메시지를 포함한 CommonResponse를 생성합니다.
    private boolean isVoidReturnType(Object body, MethodParameter returnType) {
        return body == null && returnType.getParameterType().equals(void.class);
    }

    // void 반환 타입에 대한 성공 응답을 생성합니다.
    private CommonResponse<?> handleVoidReturnType() {
        return CommonResponse.success(
                new SuccessDetails(
                        "SUCCESS",
                        "요청이 성공적으로 처리되었지만 반환할 데이터가 없습니다."));
    }

    // 특정 경로를 제외하는 메소드
    // Spring Actuator 엔드포인트 및 Spring REST Docs 관련 경로를 제외합니다.
    // 이 메소드는 경로가 null인 경우 false를 반환합니다.
    private boolean isExcludedPath(String path) {
        // 경로가 null인 경우 false 반환
        if (path == null) {
            return false;
        }
        // Spring Actuator 엔드포인트
        if (path.startsWith("/actuator")) {
            return true;
        }

        // Spring REST Docs 관련 경로
        return path.contains("docs") || path.contains("generated-docs") || path.contains("api-documentation") ||
                path.contains("webjars") || path.contains("reports/tests");
    }
}
