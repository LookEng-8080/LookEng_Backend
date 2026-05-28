# CLAUDE.md — LookEng_Backend

## Tech Stack

- Java 17, Spring Boot 4.0.5
- Spring Data JPA + Hibernate, MySQL (런타임), H2 (테스트)
- Spring Security (CSRF 비활성화, 세션 기반 인증)
- Spring Mail (Gmail SMTP — 비밀번호 재설정 토큰 발송)
- Google API Client 2.2.0 (구글 소셜 로그인 JWT 검증)
- Lombok, Bean Validation

## Package Layout

```
com.sw8080.lookeng/
├── controller/
│   ├── AuthController          # /api/v1/auth — 회원가입, 로그인, 로그아웃, 탈퇴, 비밀번호 재설정, 소셜 로그인
│   ├── WordController          # /api/v1/words — 단어 CRUD, 검색, CSV 업로드
│   ├── UserWordController      # /api/v1/user/words — 북마크/암기 토글 및 목록
│   ├── TestSessionController   # /api/v1/test/sessions — 테스트 세션 관리
│   ├── ProgressController      # /api/v1/user/progress — 진도율·등급 조회
│   └── AdminController         # /api/v1/admin — 사용자 목록, 테스트 세션 상세(관리자용)
├── service/
│   ├── AuthService, WordService, UserWordService
│   ├── TestSessionService, ProgressService, AdminService
├── repository/
│   ├── UserRepository, WordRepository, UserWordRepository
│   ├── TestSessionRepository, TestAnswerRepository, PasswordResetTokenRepository
├── entity/
│   ├── User, Word, UserWord, TestSession, TestAnswer
│   ├── PasswordResetToken, BaseTimeEntity
│   └── QuizType.java           # SHORT_ANSWER | MULTIPLE_CHOICE | FILL_IN_BLANK
├── dto/
│   ├── request/                # *RequestDto
│   └── response/               # *ResponseDto (from() 팩토리 메서드 사용)
├── exception/                  # BusinessException 계층
├── ApiResponse.java            # 공통 응답 래퍼 (success, message, data<T>)
├── GlobalExceptionHandler      # @RestControllerAdvice
├── SecurityConfig              # Spring Security + CORS 설정
└── Role.java                   # USER / ADMIN enum
```

## 인증 방식

JWT 없이 **HTTP 세션** 사용. 로그인 성공 시 세션에 두 값 저장:
- `LOGIN_USER_ID` (Long) — 유저 식별자
- `LOGIN_USER_ROLE` (Role enum) — 권한

세션 만료: 30분 (`setMaxInactiveInterval(1800)`)

Spring Security는 모든 `/api/v1/**`를 `permitAll()`로 열어두고, 실제 인증/권한 검사는 **각 컨트롤러에서 세션을 직접 읽어** 처리한다.

## CORS 허용 Origin

```java
// SecurityConfig.java
config.setAllowedOrigins(List.of(
    "null",                    // 로컬 파일 직접 열기
    "http://localhost:3000",
    "http://127.0.0.1:5500",   // VS Code Live Server
    "http://localhost:5500"
));
config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
config.setAllowCredentials(true);   // JSESSIONID 쿠키 전송 허용
```

## 공통 응답 형식

```json
{ "success": true, "message": "...", "data": { ... } }
```

`ApiResponse<T>` (대부분)와 `CommonResponse<T>` (일부 auth 엔드포인트) 두 클래스가 같은 구조로 혼용. 새로 작성할 때는 `ApiResponse<T>` 사용.

## 환경 설정

### application.yml (주설정)

| 항목 | 설명 |
|------|------|
| `spring.profiles.active` | 기본값 `dev` |
| `spring.datasource.url` | `${DB_URL}` 환경변수 |
| `spring.datasource.username` | `${DB_USERNAME}` 환경변수 |
| `spring.datasource.password` | `${DB_PASSWORD}` 환경변수 |
| `spring.jpa.hibernate.ddl-auto` | 기본 `validate`, dev 프로필은 `update` |
| `spring.mail.*` | Gmail SMTP (sw.lookeng@gmail.com) |
| `lookeng.admin.secret-code` | `"LOOKENG_SECRET_777"` — 관리자 회원가입 코드 |
| `lookeng.oauth2.google.client-id` | 구글 OAuth2 클라이언트 ID |

### application.properties (추가 설정)

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

### 프로필별 차이

| 프로필 | ddl-auto | show-sql | session cookie |
|--------|----------|----------|----------------|
| dev | update | true | same-site=lax, secure=false |
| prod | validate | false | — |

### 로컬 실행 시 필수 환경변수

```bash
DB_URL=jdbc:mysql://localhost:3306/<db명>
DB_USERNAME=...
DB_PASSWORD=...
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

- **Javadoc 사용 금지**
- 메서드 내부 단계는 번호 주석으로 구분:

```java
// 1. 중복 이메일 검증
// 2. 엔티티 생성
// 3. 응답 DTO 변환
```

- 에러메시지, 주석 모두 **한국어** 사용
- 임시 로직은 `// TODO:` 또는 이유 설명 주석 필수

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

## PR / 이슈 이력 요약

| PR | 내용 |
|----|------|
| #4 | feat: auth 개발 (회원가입, 로그인, 로그아웃) |
| #6 | feat: 단어장 개발 (단어 CRUD) |
| #8 | feat: 테스트 기능 개발 (세션 시작·답안·종료·기록) |
| #10 | chore: dev/prod 프로파일 분리, env로 DB 설정 분리 |
| #13 | refactor: 커스텀 예외 계층 도입 (BusinessException 하위) |
| #17 | feat: 입력 검증 강화, 서비스 단위 테스트 32개, CORS 설정 |
| #19 | feat: 진도율 API 개발 (isMemorized 기반 레벨 계산) |
| #22, #24 | feat: 관리자 유저 목록 API, ADMIN 제외 필터 수정 |
| #27, #28 | refactor: 단어 삭제 시 FK 오류 해결 — Soft Delete 도입 |
| #30 | fix: Word Soft Delete 시 FK 제약 오류 수정 |
| #32 | fix: 삭제된 단어 재등록 불가 오류 수정 |
| #35 | feat: 관리자 회원가입 (관리자 코드 반영) |
| #36 | feat: USER_WORD 북마크/암기 API 구현 |
| #38 | feat: CSV 단어 일괄 추가 API (1000개 제한, 헤더 검증) |
| #40 | feat: 단어 검색 API (영어/한글 부분 일치) |
| #42 | feat: 회원 탈퇴 API |
| #44 | feat: 비밀번호 재설정 API (이메일 토큰, 3분 유효) |
| #46 | fix: 북마크/암기완료 목록 응답에 예문 누락 수정 |
| #49 | fix: CSV 업로드 시 소프트딜리트된 단어 복구 처리 |
| #56 | chore: Java 툴체인 고정 제거 |
| #57 | feat: 구글 소셜 로그인 (Google JWT 검증, 자동 가입) |
| #58 | feat: 진도율 계산 로직 UserWord.isMemorized 기반으로 변경 |
| #59 | feat: FILL_IN_BLANK 퀴즈 유형 추가, 4지선다 choices 생성 |
| #60 | fix: SHORT_ANSWER 예문에서 정답 단어 마스킹 처리 |
| #61 | feat: 테스트 세션 상세 조회 API |
| — | fix: 테스트 기록 조회 시 완료된 세션만 반환 |
| — | fix: 관리자 전용 테스트 세션 상세 조회 API 추가 |
