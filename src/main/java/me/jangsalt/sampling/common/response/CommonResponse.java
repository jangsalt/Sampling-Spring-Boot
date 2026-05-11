package me.jangsalt.sampling.common.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 모든 REST 응답을 감싸는 공통 envelope.
 *
 * <p>구성:</p>
 * <ul>
 *   <li>{@code code} — HTTP 상태 코드 (200, 404, 500 등)</li>
 *   <li>{@code message} — 성공 시 "OK", 실패 시 {@code [에러코드] 메시지 (디테일)}</li>
 *   <li>{@code data} — 실제 응답 페이로드 (실패 시 {@code null})</li>
 *   <li>{@code time} — 응답 생성 시각 (ISO-8601)</li>
 * </ul>
 *
 * <p>일반 컨트롤러 응답은 {@link CommonResponseAdvice}가 자동으로 감싸고,
 * 예외 응답은 핸들러에서 {@link #error(int, String)}으로 직접 생성한다.</p>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final LocalDateTime time;

    public static <T> CommonResponse<T> success(int code, T data) {
        return new CommonResponse<>(code, "OK", data, LocalDateTime.now());
    }

    public static CommonResponse<Void> error(int code, String message) {
        return new CommonResponse<>(code, message, null, LocalDateTime.now());
    }
}
