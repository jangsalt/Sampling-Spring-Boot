package me.jangsalt.sampling.common.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.jangsalt.sampling.common.exception.BusinessException;
import me.jangsalt.sampling.common.exception.ErrorCode;
import me.jangsalt.sampling.common.response.CommonResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 비즈니스 예외 전담 핸들러.
 *
 * <p>{@link BusinessException}만 처리하며, {@link Order} 우선순위를 가장 높여
 * {@code GlobalExceptionHandler}의 catch-all에 잡히기 전에 먼저 동작한다.</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handle(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        String detail = !ex.getMessage().equals(errorCode.getMessage()) ? ex.getMessage() : null;
        String message = ErrorMessageFormatter.format(errorCode, detail);

        log.warn("BusinessException: {} | path={}", message, request.getRequestURI());

        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.error(errorCode.getStatus().value(), message));
    }
}
