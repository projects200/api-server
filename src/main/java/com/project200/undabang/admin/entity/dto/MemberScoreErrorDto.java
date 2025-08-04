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
}
