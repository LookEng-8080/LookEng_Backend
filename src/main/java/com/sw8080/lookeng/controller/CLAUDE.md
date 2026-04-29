# CLAUDE.md — controller/

## API 엔드포인트 목록

### AuthController (`/api/v1/auth`)

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/signup` | 일반 회원가입 | X |
| POST | `/admin/signup` | 관리자 회원가입 | X |
| POST | `/login` | 로그인 → JSESSIONID 발급 | X |
| POST | `/logout` | 세션 무효화 | O |

### WordController (`/api/v1/words`)

| Method | Path | 설명 | 필요 권한 |
|--------|------|------|-----------|
| POST | `/` | 단어 추가 | ADMIN |
| PATCH | `/{id}` | 단어 수정 | ADMIN |
| DELETE | `/{id}` | 단어 삭제 | ADMIN |
| GET | `/` | 단어 목록 조회 (페이지네이션) | USER 이상 |
| GET | `/{id}` | 단어 상세 조회 | USER 이상 |

단어 목록 쿼리 파라미터: `page`(기본 0), `size`(기본 20), `sort`(기본 `id,asc` / `english,asc` / `english,desc`)

### TestSessionController (`/api/v1/test/sessions`)

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/` | 테스트 세션 시작 | O |
| POST | `/{sessionId}/answers` | 답안 제출 | O |
| POST | `/{sessionId}/finish` | 테스트 종료 | O |
| GET | `/` | 테스트 기록 조회 (페이지네이션) | O |

## 컨트롤러 공통 패턴

세션 인증 확인 코드가 각 컨트롤러 메서드 상단에 반복된다:

```java
HttpSession session = httpRequest.getSession(false);
if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
}
```

ADMIN 권한 확인:
```java
Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
String role = roleObj != null ? roleObj.toString() : "";
if (!"ADMIN".equals(role)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
}
```

비즈니스 예외(`BusinessException` 하위)는 서비스에서 throw → `GlobalExceptionHandler`에서 처리되므로 컨트롤러에서 try-catch 불필요.
