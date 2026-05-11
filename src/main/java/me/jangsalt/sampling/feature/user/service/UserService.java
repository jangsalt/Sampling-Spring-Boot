package me.jangsalt.sampling.feature.user.service;

import jakarta.annotation.PostConstruct;
import me.jangsalt.sampling.common.exception.BusinessException;
import me.jangsalt.sampling.common.exception.ErrorCode;
import me.jangsalt.sampling.feature.user.entity.User;
import me.jangsalt.sampling.feature.user.dto.UserDTO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> users = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 애플리케이션 기동 시 샘플 사용자 데이터를 12명 적재한다.
     */
    @PostConstruct
    void init() {
        save("홍길동", "hong@example.com",   "010-1234-5678", "900101-1234567", "1234-5678-9012-3456", "secret123", "서울시 강남구 테헤란로 123");
        save("김철수", "kim@example.com",    "010-9876-5432", "851225-1345678", "9876-5432-1098-7654", "p@ssw0rd",  "서울시 마포구 월드컵로 456");
        save("이영희", "lee@example.com",    "010-2222-3333", "920315-2456789", "1111-2222-3333-4444", "qwer1234",  "부산시 해운대구 센텀로 78");
        save("박지민", "park@example.com",   "010-4444-5555", "880722-2567890", "5555-6666-7777-8888", "minpark11", "대구시 수성구 동대구로 91");
        save("최민수", "choi@example.com",   "010-6666-7777", "950101-1678901", "9999-8888-7777-6666", "ms@choi",   "인천시 연수구 송도과학로 200");
        save("정수연", "jung@example.com",   "010-8888-9999", "910505-2789012", "4321-8765-2109-6543", "sooyeon!",  "광주시 서구 상무대로 350");
        save("강민호", "kang@example.com",   "010-1111-2222", "870909-1890123", "1357-2468-1357-2468", "kmh1987",   "대전시 유성구 대학로 99");
        save("윤서아", "yoon@example.com",   "010-3333-4444", "961212-2901234", "2468-1357-2468-1357", "seoa**",    "울산시 남구 삼산로 12");
        save("조현우", "cho@example.com",    "010-5555-6666", "830404-1012345", "9753-1864-9753-1864", "hyunwoo7",  "세종시 한누리대로 2000");
        save("한지영", "han@example.com",    "010-7777-8888", "990818-2123456", "8642-9731-8642-9731", "jyhan99",   "경기도 성남시 분당구 판교로 100");
        save("임도윤", "lim@example.com",    "010-1212-3434", "780531-1234509", "1928-3746-5564-7382", "doyoon7",   "강원도 춘천시 중앙로 50");
        save("송예린", "song@example.com",   "010-5656-7878", "020707-3345670", "5647-8392-1029-4756", "yerinS@",   "제주시 연동 신대로 21");
    }

    private void save(String name, String email, String phone, String rrn, String cardNumber, String password, String address) {
        long id = sequence.incrementAndGet();
        users.put(id, User.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .rrn(rrn)
                .cardNumber(cardNumber)
                .password(password)
                .address(address)
                .build());
    }

    /**
     * 전체 사용자 목록을 ID 오름차순으로 반환한다.
     */
    public List<UserDTO.Response> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(User::toResponse)
                .toList();
    }

    /**
     * ID로 단일 사용자를 조회한다. 존재하지 않으면 404를 던진다.
     */
    public UserDTO.Response findById(Long id) {
        User user = users.get(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "id=" + id);
        }
        return user.toResponse();
    }

    /**
     * 검색 조건에 일치하는 사용자 목록을 반환한다.
     * {@link UserDTO.Search}의 각 필드는 nullable 이며, 값이 있을 때만 부분 일치(대소문자 무시)로 필터된다.
     * 검색 조건이 모두 비어 있으면 전체 목록을 반환한다.
     */
    public List<UserDTO.Response> search(UserDTO.Search condition) {
        if (condition == null) {
            return findAll();
        }
        String name = normalize(condition.getName());
        String email = normalize(condition.getEmail());
        if (name == null && email == null) {
            return findAll();
        }
        return users.values().stream()
                .filter(u -> name == null || u.getName().toLowerCase().contains(name))
                .filter(u -> email == null || u.getEmail().toLowerCase().contains(email))
                .sorted(Comparator.comparing(User::getId))
                .map(User::toResponse)
                .toList();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.toLowerCase();
    }

    /**
     * 신규 사용자를 등록한다. 발급된 ID를 포함한 응답을 반환한다.
     */
    public UserDTO.Response create(UserDTO.Request request) {
        long id = sequence.incrementAndGet();
        User user = User.builder()
                .id(id)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .rrn(request.getRrn())
                .cardNumber(request.getCardNumber())
                .password(request.getPassword())
                .address(request.getAddress())
                .build();
        users.put(id, user);
        return user.toResponse();
    }
}
