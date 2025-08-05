package com.project200.undabang.admin.entity.dto;

import com.project200.undabang.admin.util.ErrorLogsUtils;
import com.project200.undabang.common.context.UserContextHolder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@SuperBuilder
public class MemberScoreErrorDto extends CommonErrorDto{
    private String requestUri; // 요청 URI
    private String httpMethod; // HTTP METHOD
    private UUID userIdentifier; // 유저 식별자 UUID

    @Override
    public String getTitle() {
        return "*!!!!!운동기록 생성시 점수 추가 로직 오류 발생!!!!!*\n";
    }

    @Override
    public String getSpecificDetails() {
        StringBuilder sb = new StringBuilder();

        sb.append("*HTTP METHOD*: ").append(this.getHttpMethod()).append("\n");
        sb.append("*REQUEST URI*: ").append(this.getRequestUri()).append("\n");
        sb.append("*점수가 증가하지 않은 회원 식별자*: \n").append(this.getUserIdentifier()).append("\n");

        return sb.toString();
    }

    public static MemberScoreErrorDto of(Throwable throwable, String serviceName, ErrorLevel errorLevel, String summary, String environment){
        UUID memberId = UserContextHolder.getUserId();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String requestUri = attributes.getRequest().getRequestURI();
        String requestMethod = attributes.getRequest().getMethod();

        String errorClassName = ErrorLogsUtils.findClassErrorHappened(throwable);
        String stackTrace = ErrorLogsUtils.getStructuredStackTrace(throwable);
        String actionGuide = ErrorLogsUtils.createActionGuide(throwable);

        return MemberScoreErrorDto.builder()
                .httpMethod(requestMethod)
                .requestUri(requestUri)
                .userIdentifier(memberId)
                .serviceName(serviceName)
                .className(errorClassName)
                .errorLevel(errorLevel)
                .summary(summary)
                .errorOccurredAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .environment(environment)
                .stackTrace(stackTrace)
                .actionGuide(actionGuide)
                .build();
    }
}
