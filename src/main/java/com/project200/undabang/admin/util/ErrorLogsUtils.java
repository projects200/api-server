package com.project200.undabang.admin.util;

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
        sb.append(String.format("`%s`\n", rootCause));
        sb.append(filteredCause);

        return sb.toString();
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
