# CLAUDE.md — LookEng_Backend

## Tech Stack

- Java 17, Spring Boot 4.0.5
- Spring Data JPA + Hibernate, MySQL
- Spring Security (CSRF 비활성화, 세션 기반 인증)
- Lombok, Bean Validation

## Package Layout

```
com.sw8080.lookeng/
├── controller/      # REST 컨트롤러 (API 진입점)
├── service/         # 비즈니스 로직
├── repository/      # Spring Data JPA 인터페이스
├── entity/          # JPA 엔티티
├── dto/
│   ├── request/     # 요청 DTO
│   └── response/    # 응답 DTO
├── exception/       # 예외 클래스
├── ApiResponse.java        # 공통 응답 래퍼
├── GlobalExceptionHandler  # @RestControllerAdvice
├── SecurityConfig          # Spring Security 설정
└── Role.java               # USER / ADMIN enum
```

## 인증 방식

JWT 없이 **HTTP 세션** 사용. 로그인 성공 시 세션에 두 값 저장:
- `LOGIN_USER_ID` (Long) — 유저 식별자
- `LOGIN_USER_ROLE` (Role enum) — 권한

Spring Security는 `/api/v1/auth/**`, `/api/v1/words/**`, `/api/v1/test/**`를 전부 `permitAll()`로 열어두고, 실제 인증/권한 검사는 **각 컨트롤러에서 세션을 직접 읽어** 처리한다.

세션 만료: 30분 (`setMaxInactiveInterval(1800)`)

## 공통 응답 형식

```json
{ "success": true, "message": "...", "data": { ... } }
```

`ApiResponse<T>` (대부분)와 `CommonResponse<T>` (일부 auth 엔드포인트) 두 클래스가 같은 구조로 혼용되고 있다.

## 환경 설정

`application.properties`에는 `spring.application.name=LookEng`만 커밋되어 있다. 로컬 실행 시 별도 파일 또는 환경변수로 아래 항목을 추가해야 한다:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<db명>
spring.datasource.username=...
spring.datasource.password=...
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```
