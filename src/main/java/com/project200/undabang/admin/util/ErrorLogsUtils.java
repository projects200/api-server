package com.project200.undabang.admin.util;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

public final class ErrorLogsUtils {
    private ErrorLogsUtils() {}
    private static final String PACKAGE_NAME = "com.project200.undabang";
    private static final int MAX_LINE_LIMIT = 10;

    // 에러가 발생한 클래스명을 반환하는 유틸 메소드
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

    // StackTrace를 호출하면서 가장 근원이 되는 에러를 개발자에게 알려주는 유틸 메소드
    public static String getStructuredStackTrace(Throwable throwable) {
        if(throwable == null){
            return "throwable 값이 없습니다.";
        }

        Throwable rootCause = getRootCause(throwable);
        String filteredCause = filteringStackTrace(rootCause);

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n *RootCause* : ");
        sb.append(rootCause).append("\n");
        sb.append(filteredCause);

        return sb.toString();
    }

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
                sb.append("    3. [설정 정보] application.yml의 DB 연결 정보(url, username, password)가 올바른지 확인하세요.");
            }

            // DB 컬럼 / 테이블 오류
            case InvalidDataAccessResourceUsageException invalidDataAccessResourceUsageException -> {
                sb.append("[컬럼, 테이블 오류 등 잘못된 DB 리소스 사용에 대한 해결방안 입니다.]\n");
                sb.append("  - 원인: 존재하지 않는 테이블이나 컬럼을 참조하는 등 SQL 문법 오류일 가능성이 높습니다.\n");
                sb.append("  - 해결 방안:\n");
                sb.append("    1. JPA Entity의 @Column 매핑이 DB 스키마와 일치하는지 확인하세요.\n");
                sb.append("    2. QueryDSL 코드에서 오타가 없는지 확인하세요.");
            }

            // 무결성 오류
            case DataIntegrityViolationException dataIntegrityViolationException -> {
                sb.append("[무결성 제약조건 위반에 대한 해결방안입니다.]\n");
                sb.append("  - 원인: Unique, Not Null, Foreign Key, 데이터 길이/타입 등 DB 제약조건을 위반했습니다.\n");
                sb.append("  - 해결 방안:\n");
                sb.append("    1. 저장하려는 데이터가 DB 스키마의 제약조건을 만족하는지 확인하세요.\n");
                sb.append("    2. Unique 제약조건이 있을 경우 중복된 데이터를 삽입하려는지 확인하세요.");
            }

            default -> {
                sb.append("[오류가 발생하였습니다.\n]");
                sb.append(" - 전체 stacktrace를 확인하여 정확한 원인을 판단하세요!");
            }
        }

        return sb.toString();
    }

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

    // 전체 에러를 보내기는 에러의 양이 너무 많으므로 운다방 패키지와 관련된 부분만 추려내서 개발자에게 전달
    private static String filteringStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        int lineCount = 0;

        for (StackTraceElement element : throwable.getStackTrace()) {
            if(element.getClassName().startsWith(PACKAGE_NAME)){
                sb.append("  at \n").append(element);
                sb.append(element).append("\n");
                lineCount++;
            }

            if(lineCount >= MAX_LINE_LIMIT){
                sb.append(" ... (생략)\n");
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
