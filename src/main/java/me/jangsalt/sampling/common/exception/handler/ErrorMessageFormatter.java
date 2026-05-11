package me.jangsalt.sampling.common.exception.handler;

import me.jangsalt.sampling.common.exception.ErrorCode;

/**
 * 에러 응답의 {@code message} 필드 포맷을 일관되게 생성하는 유틸.
 *
 * <p>형식: {@code [에러코드] 표준 메시지 (디테일)}</p>
 *
 * <pre>{@code
 * format(USER_NOT_FOUND, "id=999")
 *   → "[U0001] 사용자를 찾을 수 없습니다. (id=999)"
 *
 * format(USER_NOT_FOUND, null)
 *   → "[U0001] 사용자를 찾을 수 없습니다."
 * }</pre>
 */
final class ErrorMessageFormatter {

    private ErrorMessageFormatter() {}

    static String format(ErrorCode errorCode, String detail) {
        String base = "[" + errorCode.getCode() + "] " + errorCode.getMessage();
        if (detail == null || detail.isBlank()) {
            return base;
        }
        return base + " (" + detail + ")";
    }
}
