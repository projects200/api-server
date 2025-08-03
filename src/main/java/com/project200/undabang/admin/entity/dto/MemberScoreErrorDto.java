package com.project200.undabang.admin.entity.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class MemberScoreErrorDto extends CommonErrorDto{
    private String requestUri; // 요청 URI
    private String httpMethod; // HTTP METHOD
    private UUID userIdentifier; // 유저 식별자 UUID

    @Override
    public String formattingMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("*!!!!!운동기록 생성시 점수 추가 로직 오류 발생!!!!!*\n");
        sb.append("------------------------------------------\n");
        sb.append("*HTTP METHOD*: ").append(this.getHttpMethod()).append("\n");
        sb.append("*REQUEST URI*: ").append(this.getRequestUri()).append("\n");
        sb.append("*점수가 증가하지 않은 회원 식별자*: \n").append(this.getUserIdentifier()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("*오류 요약*: ").append(super.getSummary()).append("\n");
        sb.append("*에러가 발생한 서비스*: ").append(super.getServiceName()).append("\n");
        sb.append("*에러가 발생한 클래스*: ").append(super.getClassName()).append("\n");
        sb.append("*에러의 심각도*: ").append(super.getErrorLevel()).append("\n");
        sb.append("*에러 발생 시간*: ").append(super.getErrorOccurredAt()).append("\n");
        sb.append("*에러 발생 지점*: ").append(super.getStackTrace()).append("\n");
        sb.append("*에러 발생 환경*: ").append(super.getEnvironment()).append("\n");
        sb.append("*개발자 행동 추천 가이드*: ").append(super.getActionGuide()).append("\n");

        return sb.toString();
    }
}
