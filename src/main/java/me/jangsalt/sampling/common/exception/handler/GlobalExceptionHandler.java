package me.jangsalt.sampling.common.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.jangsalt.sampling.common.exception.ErrorCode;
import me.jangsalt.sampling.common.response.CommonResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 비즈니스 예외 외 나머지 예외(프레임워크/JDK/알 수 없음)를 처리하는 전역 핸들러.
 *
 * <p>{@code BusinessExceptionHandler}보다 낮은 우선순위로 동작하여 비즈니스 예외에는 영향을 주지 않는다.
 * 잘못된 입력/메서드 등 클라이언트 측 원인은 로그 레벨을 낮추고, 알 수 없는 예외만 ERROR로 기록한다.</p>
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<CommonResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.info("BadRequest: type={}, message={}", ex.getClass().getSimpleName(), ex.getMessage());
        return build(ErrorCode.INVALID_INPUT, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.info("MethodNotAllowed: {}", ex.getMessage());
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at path={}", request.getRequestURI(), ex);
        // 보안상 내부 메시지를 응답에 노출하지 않는다.
        return build(ErrorCode.INTERNAL_ERROR, null, request);
    }

    private static ResponseEntity<CommonResponse<Void>> build(ErrorCode errorCode, String detail, HttpServletRequest request) {
        String message = ErrorMessageFormatter.format(errorCode, detail);
        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.error(errorCode.getStatus().value(), message));
    }
}
