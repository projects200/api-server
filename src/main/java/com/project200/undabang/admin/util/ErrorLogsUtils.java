package com.project200.undabang.admin.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ErrorLogsUtils {
    private ErrorLogsUtils() {}

    public static String getStructuredStackTrace(Throwable throwable) {
        Throwable rootCause = getRootCause(throwable);
        String rootCauseStackTrace = convertThrowableToString(rootCause);

        String fullTrace = convertThrowableToString(throwable);

        StringBuilder sb = new StringBuilder();
        sb.append("!!!! *Root Cause* !!!! \n");
        sb.append(rootCauseStackTrace);
        sb.append("\n\n");

        if(!rootCauseStackTrace.equals(fullTrace)) {
            sb.append("!!!! *Full Trace* !!!! \n");
            sb.append(fullTrace);
            sb.append("\n\n");
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

    private static String convertThrowableToString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return printWriter.toString();
    }




}
