package me.jangsalt.sampling.feature.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.jangsalt.sampling.common.component.mask.Mask;
import me.jangsalt.sampling.common.component.mask.MaskType;

public class UserDTO {

    private UserDTO() {}

    /**
     * 사용자 생성/수정 요청 DTO.
     * 입력 값은 마스킹하지 않고 원본 그대로 받는다.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        private String name;

        private String email;

        private String phone;

        private String rrn;

        private String cardNumber;

        private String password;

        private String address;
    }

    /**
     * 사용자 조회 조건 DTO.
     * 쿼리 파라미터로부터 자동 바인딩된다 (예: {@code GET /api/users?name=홍&email=hong}).
     * 모든 필드는 nullable 이며 값이 있을 때만 검색 조건으로 사용된다.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {

        private String name;

        private String email;
    }

    /**
     * 사용자 조회 응답 DTO.
     * {@link Mask} 어노테이션이 부착된 필드는 직렬화 시 자동 마스킹된다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private Long id;

        @Mask(MaskType.NAME)
        private String name;

        @Mask(MaskType.EMAIL)
        private String email;

        @Mask(MaskType.PHONE)
        private String phone;

        @Mask(MaskType.RRN)
        private String rrn;

        @Mask(MaskType.CARD)
        private String cardNumber;

        @Mask
        private String password;

        private String address;
    }
}
