package com.project200.undabang.common.context;

import java.util.UUID;

/**
 * 현재 실행 스레드에 사용자 ID를 저장하고 검색하기 위한 유틸리티 클래스입니다.
 * <p>
 * 이 클래스는 웹 요청 또는 비동기 작업 동안 사용자 컨텍스트를 유지하기 위해 ThreadLocal을 사용합니다.
 * 각 스레드는 독립적인 사용자 컨텍스트를 가지므로, 멀티스레드 환경에서 사용자 정보가 서로 간섭하지 않습니다.
 * </p>
 * <p>
 * 사용 예시:
 * <pre>
 *     // 사용자 ID 설정
 *     UserContextHolder.setUserId(userId);
 *
 *     // 작업 수행
 *     ...
 *
 *     // 다른 곳에서 사용자 ID 획득
 *     UUID currentUserId = UserContextHolder.getUserId();
 *
 *     // 작업 완료 후 컨텍스트 정리
 *     UserContextHolder.reset();
 * </pre>
 * </p>
 * <p>
 * 주의: 스레드가 재사용되는 환경(예: 서블릿 컨테이너)에서는 요청 처리가 완료된 후
 * 반드시 {@link #reset()} 메서드를 호출하여 ThreadLocal 값을 정리해야 합니다.
 * 그렇지 않으면 메모리 누수가 발생할 수 있습니다.
 * </p>
 */

public class UserContextHolder {
    private static final ThreadLocal<UUID> userContext = new ThreadLocal<>();
    private static final ThreadLocal<String> userEmailContext = new ThreadLocal<>();

    /**
     * 유틸리티 클래스의 인스턴스화를 방지합니다.
     *
     * @throws IllegalStateException 생성자가 호출된 경우 항상 발생
     */
    private UserContextHolder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 현재 스레드의 사용자 ID를 설정합니다.
     *
     * @param userId 현재 스레드에 연결할 사용자의 고유 식별자
     */
    public static void setUserId(UUID userId) {
        userContext.set(userId);
    }

    /**
     * 현재 스레드에 연결된 사용자 ID를 반환합니다.
     *
     * @return 현재 사용자의 UUID, 설정되지 않은 경우 null 반환
     */
    public static UUID getUserId() {
        return userContext.get();
    }

    public static void setUserEmail(String userEmail){
        userEmailContext.set(userEmail);
    }

    public static String getUserEmail(){
        return userEmailContext.get();
    }

    /**
     * 현재 스레드에서 사용자 컨텍스트를 제거합니다.
     * 스레드 풀 환경에서는 요청 처리 완료 후 항상 이 메서드를 호출하여
     * ThreadLocal 리소스 누수를 방지해야 합니다.
     */
    public static void reset() {
        userContext.remove();
        userEmailContext.remove();
    }
}
