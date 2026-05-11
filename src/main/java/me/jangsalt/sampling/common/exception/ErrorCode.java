package me.jangsalt.sampling.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전반에서 사용하는 표준 에러 코드.
 *
 * <p>코드 체계:</p>
 * <ul>
 *   <li>{@code E0xxx} — 공통 (Common)</li>
 *   <li>{@code U0xxx} — User 도메인</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    INVALID_INPUT      ("E0001", "잘못된 입력입니다.",              HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND ("E0404", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED ("E0405", "허용되지 않은 메서드입니다.",       HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_ERROR     ("E0500", "서버 내부 오류가 발생했습니다.",    HttpStatus.INTERNAL_SERVER_ERROR),

    USER_NOT_FOUND     ("U0001", "사용자를 찾을 수 없습니다.",        HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
