package com.project200.undabang.member.dto.event;

import java.util.UUID;

/**
 * 회원 가입 이벤트를 나타내는 클래스입니다.
 * 회원이 새로 가입되었음을 나타내며, 가입된 회원의 고유 식별자를 포함합니다.
 * <p>
 * 이 클래스는 이벤트 핸들링 및 전파를 위해 사용됩니다.
 */
public record MemberSignedUpEvent(UUID memberId) {
}
