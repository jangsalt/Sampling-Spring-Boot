package me.jangsalt.sampling.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 {@code @RestController} 응답을 {@link CommonResponse}로 자동 래핑한다.
 *
 * <p>이미 {@link CommonResponse}이거나 {@code String} 반환은 건너뛴다.
 * String은 {@code StringHttpMessageConverter}가 사용되어 객체 직렬화가 정상 동작하지 않기 때문이다.</p>
 *
 * <p>예외 핸들러가 {@link CommonResponse#error(int, String)}로 생성한 응답은
 * {@code supports()}에서 false를 반환해 이중 래핑을 방지한다.</p>
 */
@RestControllerAdvice
public class CommonResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !CommonResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof CommonResponse<?>) {
            return body;
        }
        if (body instanceof String) {
            return body;   // StringHttpMessageConverter 경로 - 래핑 불가
        }
        return CommonResponse.success(currentStatus(response), body);
    }

    private static int currentStatus(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servlet) {
            int status = servlet.getServletResponse().getStatus();
            return status != 0 ? status : HttpStatus.OK.value();
        }
        return HttpStatus.OK.value();
    }
}
