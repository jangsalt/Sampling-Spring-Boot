package me.jangsalt.sampling.common.component.mask;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 요청의 {@code mask} 쿼리 파라미터를 읽어 {@link MaskingContext}를 토글하는 인터셉터.
 *
 * <ul>
 *   <li>{@code ?mask=false} → 마스킹 해제 (원본 값 노출)</li>
 *   <li>그 외 (없거나 다른 값) → 마스킹 적용 (기본)</li>
 * </ul>
 *
 * <p>ThreadLocal 누수를 방지하기 위해 {@link #afterCompletion} 시점에 컨텍스트를 정리한다.</p>
 */
@Component
public class MaskingInterceptor implements HandlerInterceptor {

    private static final String MASK_PARAM = "mask";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String maskParam = request.getParameter(MASK_PARAM);
        if ("false".equalsIgnoreCase(maskParam)) {
            MaskingContext.disableMasking();
        } else {
            MaskingContext.enableMasking();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MaskingContext.clear();
    }
}
