package me.jangsalt.sampling.common.component.mask;

/**
 * 문자열 값을 {@link MaskType}에 따라 마스킹 처리하는 유틸리티 클래스.
 *
 * <p>{@link MaskSerializer}를 통해 Jackson 직렬화 시점에 자동 호출되며,
 * 필요 시 직접 호출하여 수동으로 마스킹할 수도 있다.</p>
 *
 * <pre>{@code
 * String masked = MaskingUtil.mask("홍길동", MaskType.NAME); // "홍*동"
 * }</pre>
 */
public final class MaskingUtil {

    private MaskingUtil() {}

    /**
     * 입력 값을 지정된 {@link MaskType}에 따라 마스킹한다.
     * {@code null}이거나 빈 문자열인 경우 원본을 그대로 반환한다.
     *
     * <p>예시:</p>
     * <pre>{@code
     * MaskingUtil.mask("홍길동",            MaskType.NAME);    // "홍*동"
     * MaskingUtil.mask("hong@example.com", MaskType.EMAIL);   // "h***@example.com"
     * MaskingUtil.mask("010-1234-5678",    MaskType.PHONE);   // "010-****-5678"
     * MaskingUtil.mask("900101-1234567",   MaskType.RRN);     // "900101-1******"
     * MaskingUtil.mask("1234-5678-9012-3456", MaskType.CARD); // "1234-****-****-3456"
     * MaskingUtil.mask("secret",           MaskType.DEFAULT); // "******"
     * }</pre>
     *
     * @param value 마스킹할 원본 문자열 (nullable)
     * @param type  적용할 마스킹 유형
     * @return 마스킹된 문자열 (원본이 null/empty면 그대로 반환)
     */
    public static String mask(String value, MaskType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (type) {
            case NAME -> maskName(value);
            case EMAIL -> maskEmail(value);
            case PHONE -> maskPhone(value);
            case RRN -> maskRrn(value);
            case CARD -> maskCard(value);
            case DEFAULT -> "*".repeat(value.length());
        };
    }

    /**
     * 이름 마스킹: 첫 글자와 마지막 글자는 노출하고 가운데를 마스킹한다.
     *
     * <p>예시:</p>
     * <pre>
     * "가"       → "*"
     * "홍길"     → "홍*"
     * "홍길동"   → "홍*동"
     * "김철수영" → "김**영"
     * </pre>
     */
    private static String maskName(String name) {
        int len = name.length();
        if (len <= 1) return "*";
        if (len == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(len - 2) + name.charAt(len - 1);
    }

    /**
     * 이메일 마스킹: 로컬 파트(@ 앞부분)의 첫 글자만 노출하고 나머지를 마스킹한다.
     * 도메인 부분은 그대로 유지한다. {@code @} 위치가 1자 이하면 원본을 반환한다.
     *
     * <p>예시:</p>
     * <pre>
     * "hong@example.com"   → "h***@example.com"
     * "kim@example.com"    → "k**@example.com"
     * "a@example.com"      → "a@example.com"   (로컬파트가 1자라 마스킹 생략)
     * </pre>
     */
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return local.charAt(0) + "*".repeat(local.length() - 1) + domain;
    }

    /**
     * 전화번호 마스킹: 숫자 기준 뒤 4자리는 노출하고 그 앞 4자리를 마스킹한다.
     * 하이픈/공백 등 구분자는 원본 그대로 유지된다. 숫자가 8자리 미만이면 원본을 반환한다.
     *
     * <p>예시:</p>
     * <pre>
     * "010-1234-5678" → "010-****-5678"
     * "01012345678"   → "010****5678"
     * "02-123-4567"   → "02-****4567"   (가운데 4자리 마스킹)
     * </pre>
     */
    private static String maskPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 8) return phone;
        int totalDigits = digits.length();
        StringBuilder sb = new StringBuilder();
        int digitIdx = 0;
        for (char c : phone.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digitIdx >= totalDigits - 8 && digitIdx < totalDigits - 4) {
                    sb.append('*');
                } else {
                    sb.append(c);
                }
                digitIdx++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 주민등록번호 마스킹: 앞 6자리와 뒤 7자리 중 첫 자리(성별 코드)까지 노출하고 나머지를 마스킹한다.
     * 하이픈 유무 모두 처리한다.
     *
     * <p>예시:</p>
     * <pre>
     * "900101-1234567" → "900101-1******"
     * "9001011234567"  → "9001011******"
     * </pre>
     */
    private static String maskRrn(String rrn) {
        int dash = rrn.indexOf('-');
        if (dash < 0) {
            if (rrn.length() < 8) return rrn;
            return rrn.substring(0, 7) + "*".repeat(rrn.length() - 7);
        }
        String front = rrn.substring(0, dash + 2);
        return front + "*".repeat(rrn.length() - front.length());
    }

    /**
     * 카드번호 마스킹: 숫자 기준 앞 4자리와 뒤 4자리는 노출하고 가운데를 모두 마스킹한다.
     * 하이픈/공백 등 구분자는 원본 그대로 유지된다. 숫자가 12자리 미만이면 원본을 반환한다.
     *
     * <p>예시:</p>
     * <pre>
     * "1234-5678-9012-3456" → "1234-****-****-3456"
     * "1234567890123456"    → "1234********3456"
     * "1234 5678 9012 3456" → "1234 **** **** 3456"
     * </pre>
     */
    private static String maskCard(String card) {
        String digits = card.replaceAll("\\D", "");
        if (digits.length() < 12) return card;
        StringBuilder sb = new StringBuilder();
        int digitIdx = 0;
        int totalDigits = digits.length();
        for (char c : card.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digitIdx >= 4 && digitIdx < totalDigits - 4) {
                    sb.append('*');
                } else {
                    sb.append(c);
                }
                digitIdx++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
