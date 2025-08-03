package com.project200.undabang.admin.util;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 예외 처리 및 오류 로깅과 관련된 유틸리티 메소드를 제공하는 클래스입니다.
 * 이 클래스는 인스턴스화할 수 없도록 final 및 private 생성자로 선언되었습니다.
 */
public final class ErrorLogsUtils {
    private ErrorLogsUtils() {}

    private static final String PACKAGE_NAME = "com.project200.undabang"; // 패키지 기본 경로
    private static final int MAX_LINE_LIMIT = 3; // 스택 트레이스에 표시할 최대 라인 수

    /**
     * 발생한 예외(Throwable)를 분석하여 오류가 시작된 애플리케이션 내부의 클래스 및 지점을 찾아 문자열로 반환합니다.
     * 스택 트레이스를 역추적하며, 프로젝트의 기본 패키지({@value #PACKAGE_NAME}) 내에서 발생한 첫 번째 지점을 식별합니다.
     *
     * @param throwable 분석할 예외 객체
     * @return {@value #PACKAGE_NAME}으로 시작하는 패키지에서 오류가 발생한 첫 번째 위치의 전체 스택 트레이스 문자열.
     *         만약 해당 패키지 내에서 원인을 찾지 못하면 스택 트레이스의 최상단 클래스 이름을 반환합니다.
     *         throwable이 null일 경우, "throwable 값이 없습니다." 메시지를 반환합니다.
     */
    public static String findClassErrorHappened(Throwable throwable){
        if(throwable == null){
            return "throwable 값이 없습니다.";
        }

        // StackTrace 를 돌면서 최초로 에러가 발생하는 지점을 찾으면 해당 클래스명을 반환해준다
        for (StackTraceElement element : throwable.getStackTrace()) {
            if(element.getClassName().startsWith(PACKAGE_NAME)){
                return element.toString();
            }
        }

        // 발견하지 못할 경우 최상위(StackTrace 의 맨 마지막 스택) 클래스명 반환
        return throwable.getStackTrace()[0].getClassName();
    }

    /**
     * 예외의 근본 원인(Root Cause)과 애플리케이션 내부와 관련된 스택 트레이스를 조합하여 구조화된 오류 메시지를 생성합니다.
     * 로그나 알림을 통해 개발자가 핵심적인 정보만 빠르게 파악할 수 있도록 돕습니다.
     *
     * @param throwable 분석할 예외 객체
     * @return 예외의 근본 원인과 필터링된 스택 트레이스가 포함된 문자열.
     *         throwable이 null일 경우, "throwable 값이 없습니다." 메시지를 반환합니다.
     */
    public static String getStructuredStackTrace(Throwable throwable) {
        if(throwable == null){
            return "throwable 값이 없습니다.";
        }

        Throwable rootCause = getRootCause(throwable);
        String filteredCause = filteringStackTrace(rootCause);

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n *RootCause* : \n");
        sb.append(rootCause).append("\n");
        sb.append(filteredCause);

        return sb.toString();
    }

    /**
     * 발생한 예외의 유형을 분석하여 개발자에게 해결을 위한 구체적인 조치 가이드를 생성합니다.
     * 특히, Spring의 {@link DataAccessException} 하위 예외들을 분석하여 일반적인 데이터베이스 관련 문제에 대한 해결 방안을 제시합니다.
     *
     * @param throwable 분석할 예외 객체
     * @return 예외 유형에 따른 해결 가이드 문자열.
     *         분석 가능한 원인이 없으면 "원인을 찾을 수 없습니다" 메시지를 반환하고,
     *         특정 가이드가 없는 예외의 경우 전체 스택 트레이스 확인을 권장하는 기본 메시지를 반환합니다.
     */
    public static String createActionGuide(Throwable throwable) {
        Throwable analyzedCause = findCauseToAnalyze(throwable);

        if(analyzedCause == null){
            return "원인을 찾을 수 없습니다";
        }

        StringBuilder sb = new StringBuilder();

        switch (analyzedCause) {
            // DB 연결 실패 오류
            case DataAccessResourceFailureException dataAccessResourceFailureException -> {
                sb.append("[데이터베이스 연결 실패 오류에 대한 해결방안 입니다.]\n");
                sb.append("  - 원인: 데이터베이스에 연결하거나 커넥션을 얻어오는데 실패했습니다.\n");
                sb.append("  - 해결 방안:\n");
                sb.append("    1. [DB 서버] 접근하려는 데이터베이스 서버가 정상 동작 중인지 확인하세요.\n");
                sb.append("    2. [네트워크/방화벽] 애플리케이션 서버와 DB 서버 간의 네트워크 연결 및 방화벽을 확인하세요.\n");
                sb.append("    3. [설정 정보] application.yml의 DB 연결 정보(url, username, password)가 올바른지 확인하세요.\n");
            }

            // DB 컬럼 / 테이블 오류
            case InvalidDataAccessResourceUsageException invalidDataAccessResourceUsageException -> {
                sb.append("[컬럼, 테이블 오류 등 잘못된 DB 리소스 사용에 대한 해결방안 입니다.]\n");
                sb.append("  - 원인: 존재하지 않는 테이블이나 컬럼을 참조하는 등 SQL 문법 오류일 가능성이 높습니다.\n");
                sb.append("  - 해결 방안:\n");
                sb.append("    1. JPA Entity의 @Column 매핑이 DB 스키마와 일치하는지 확인하세요.\n");
                sb.append("    2. QueryDSL 코드에서 오타가 없는지 확인하세요.\n");
            }

            // 무결성 오류
            case DataIntegrityViolationException dataIntegrityViolationException -> {
                sb.append("[무결성 제약조건 위반에 대한 해결방안입니다.]\n");
                sb.append("  - 원인: Unique, Not Null, Foreign Key, 데이터 길이/타입 등 DB 제약조건을 위반했습니다.\n");
                sb.append("  - 해결 방안:\n");
                sb.append("    1. 저장하려는 데이터가 DB 스키마의 제약조건을 만족하는지 확인하세요.\n");
                sb.append("    2. Unique 제약조건이 있을 경우 중복된 데이터를 삽입하려는지 확인하세요.\n");
            }

            default -> {
                sb.append("[오류가 발생하였습니다.]\n");
                sb.append(" 전체 stacktrace를 확인하여 정확한 원인을 판단하세요!\n");
            }
        }

        return sb.toString();
    }

    /**
     * 조치 가이드를 생성하기 위해 예외 체인을 탐색하며 분석할 원인을 찾습니다.
     * 현재는 {@link DataAccessException}을 주요 분석 대상으로 찾으며, 필요시 다른 종류의 예외를 추가하여 확장할 수 있습니다.
     *
     * @param throwable 분석을 시작할 최상위 예외 객체
     * @return 분석에 가장 적합한 원인(Cause) 예외. 특정 원인을 찾지 못하면 예외 체인의 가장 근본적인 원인(Root Cause)을 반환합니다.
     */
    private static Throwable findCauseToAnalyze(Throwable throwable) {
        Throwable currentThrowable = throwable;

        while(currentThrowable != null){

            // 스프링 DB 예외
            if(currentThrowable instanceof DataAccessException){
                return currentThrowable;
            }
            // 그 후, 추가적인 예외를 발견시 이곳에 추가하여 사용하면 됨

            currentThrowable = currentThrowable.getCause();
        }

        return getRootCause(throwable);
    }

    /**
     * 전체 스택 트레이스에서 {@value #PACKAGE_NAME} 패키지 경로를 포함하는 부분만 필터링하여 문자열로 반환합니다.
     * 이는 로그가 외부 라이브러리 정보로 과도하게 채워지는 것을 방지하고, 애플리케이션 내부의 문제에 집중할 수 있도록 돕습니다.
     *
     * @param throwable 필터링할 스택 트레이스를 가진 예외 객체
     * @return 필터링 및 축약된 스택 트레이스 문자열. 최대 {@value #MAX_LINE_LIMIT}개의 라인만 포함됩니다.
     */
    private static String filteringStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        int lineCount = 0;

        // findClassErrorHappened 메소드와 기능이 중복되지만, 스택 트레이스를 추출하기 위해 중복사용
        for (StackTraceElement element : throwable.getStackTrace()) {
            if(element.getClassName().startsWith(PACKAGE_NAME)){
                sb.append("\n  at \n").append(element);
                sb.append(element).append("\n");
                lineCount++;
            }

            if(lineCount >= MAX_LINE_LIMIT){
                sb.append(" ... \n");
                break;
            }
        }
        return sb.toString();
    }

    // 최상위 원인을 찾는 헬퍼 메소드
    private static Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        // 최상위 원인 찾기
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}
