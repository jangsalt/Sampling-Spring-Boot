package me.jangsalt.sampling.common.exception;

import lombok.Getter;

/**
 * 비즈니스(내부 로직) 예외 베이스 클래스.
 *
 * <p>서비스 계층에서 도메인 규칙 위반/리소스 없음 등의 상황을 표현할 때 사용한다.
 * Spring/JDK 표준 예외와 구분되어 {@code BusinessExceptionHandler}에서 일관된 형식으로 처리된다.</p>
 *
 * <pre>{@code
 * throw new BusinessException(ErrorCode.USER_NOT_FOUND, "id=" + id);
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
