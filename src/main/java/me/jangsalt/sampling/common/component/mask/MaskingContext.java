package me.jangsalt.sampling.common.component.mask;

/**
 * 현재 요청 스레드의 마스킹 활성 상태를 보관하는 ThreadLocal 컨텍스트.
 *
 * <p>{@link MaskSerializer}가 직렬화 시점에 이 값을 조회하여 마스킹 적용 여부를 결정한다.
 * 기본값은 {@code true} (마스킹 적용)이며, 요청 단위로 토글된다.</p>
 *
 * <p>일반적으로 {@code MaskingInterceptor}에서 요청 시작 시 설정하고
 * 요청 종료 시 {@link #clear()}로 정리한다.</p>
 */
public final class MaskingContext {

    private static final ThreadLocal<Boolean> MASK_ENABLED = ThreadLocal.withInitial(() -> true);

    private MaskingContext() {}

    /** 현재 스레드의 마스킹 활성 상태 (기본 {@code true}). */
    public static boolean isMaskingEnabled() {
        return MASK_ENABLED.get();
    }

    /** 마스킹을 활성화한다. */
    public static void enableMasking() {
        MASK_ENABLED.set(true);
    }

    /** 마스킹을 비활성화한다 (원본 값 노출). */
    public static void disableMasking() {
        MASK_ENABLED.set(false);
    }

    /** ThreadLocal 누수를 방지하기 위해 요청 종료 시 호출한다. */
    public static void clear() {
        MASK_ENABLED.remove();
    }
}
