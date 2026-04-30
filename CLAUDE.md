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

## 코딩 컨벤션

### 네이밍

- **변수/필드/메서드**: `camelCase`
- **클래스/DTO**: `PascalCase`
- **boolean 필드**: `is` 접두사 — `isCorrect`, `isFinished`, `isMemorized`
- **상수**: Enum으로 정의 (`Role.USER`, `QuizType.SHORT_ANSWER`)
- **DTO**: `*RequestDto` / `*ResponseDto` — `request/`, `response/` 디렉토리에 분리
- **Service 메서드 동사**: 생성 `create*`, 수정 `update*`, 삭제 `delete*`, 조회 `get*`
- **특수 필드명**: `passwordHash`, `createdAt`, `updatedAt`, `deletedAt` (접미사 패턴 유지)

### 들여쓰기 / 포맷

- **4칸 스페이스** (탭 사용 금지)
- 메서드 체인 (Builder 등)은 한 줄씩 내려쓰고 8칸 들여쓰기:

```java
User user = User.builder()
        .email(request.getEmail())
        .passwordHash(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .build();
```

- 메서드 간 빈 줄 1줄, 메서드 내 논리 블록 간 빈 줄 1줄

### 주석

- **Javadoc 사용 금지** — 메서드/클래스 레벨 Javadoc 없음
- 메서드 내부 단계는 번호 주석으로 구분:

```java
// 1. 중복 이메일 검증
// 2. 엔티티 생성
// 3. 응답 DTO 변환
```

- 에러메시지, 주석 모두 **한국어** 사용
- 임시 로직은 `// TODO:` 또는 이유 설명 주석 필수: `// USER_WORD 테이블 미구현으로 임시 false`

### 의존성 주입 / Lombok

- `@Autowired` 금지 — `@RequiredArgsConstructor` + `final` 필드로만 주입
- `@Slf4j` 미사용 — 로깅 불필요 시 추가하지 않음
- Entity: `@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @Builder`
- DTO: `@Getter @NoArgsConstructor @AllArgsConstructor @Builder`

### Null 처리

- **Service**: `Optional.orElseThrow()` 사용

```java
Word word = wordRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));
```

- **Controller**: 세션 체크는 명시적 null 비교
- **Entity update()**: null이 아닌 필드만 선택 반영 (PATCH 동작)

```java
if (english != null) this.english = english;
```

### DTO 변환

DTO에 정적 팩토리 메서드 `from()` 작성 — 컨트롤러/서비스에서 직접 Builder 호출 금지:

```java
public static WordResponseDto from(Word word) {
    return WordResponseDto.builder()
            .id(word.getId())
            .english(word.getEnglish())
            .build();
}
```

### 트랜잭션

- 조회 메서드: `@Transactional(readOnly = true)`
- 변경 메서드: `@Transactional`
- Controller에서 `@Transactional` 사용 금지 — Service에서만
