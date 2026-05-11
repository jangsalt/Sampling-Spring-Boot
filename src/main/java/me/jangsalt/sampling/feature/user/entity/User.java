package me.jangsalt.sampling.feature.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import me.jangsalt.sampling.feature.user.dto.UserDTO;

/**
 * 사용자 엔티티.
 *
 * <p>서비스 계층에서 사용하는 내부 표현으로, 마스킹 같은 표현 계층 관심사는 포함하지 않는다.
 * 응답 시 {@link #toResponse()}로 {@link UserDTO.Response}로 변환한다.</p>
 */
@Getter
@Builder
@AllArgsConstructor
public class User {

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String rrn;
    private final String cardNumber;
    private final String password;
    private final String address;

    public UserDTO.Response toResponse() {
        return UserDTO.Response.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .rrn(rrn)
                .cardNumber(cardNumber)
                .password(password)
                .address(address)
                .build();
    }
}
