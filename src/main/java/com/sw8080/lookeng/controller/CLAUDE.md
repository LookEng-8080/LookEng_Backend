# CLAUDE.md — controller/

## API 엔드포인트 전체 목록

### AuthController (`/api/v1/auth`)

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/signup` | 일반 사용자 회원가입 | X |
| POST | `/admin/signup` | 관리자 회원가입 (관리자 코드 검증) | X |
| POST | `/login` | 로그인 → JSESSIONID 발급, 세션에 LOGIN_USER_ID/ROLE 저장 | X |
| POST | `/logout` | 세션 무효화 | O |
| DELETE | `/withdraw` | 회원 탈퇴 (USER 역할만 허용, ADMIN 차단) | O |
| POST | `/password/reset-request` | 비밀번호 재설정 요청 — 5자리 토큰 이메일 발송 (3분 유효) | X |
| POST | `/password/reset` | 비밀번호 재설정 — 토큰 검증 후 변경 | X |
| POST | `/social` | 구글 소셜 로그인 — Google ID Token 검증, 자동 가입 | X |

### WordController (`/api/v1/words`)

| Method | Path | 설명 | 필요 권한 |
|--------|------|------|-----------|
| GET | `/` | 단어 목록 조회 (페이지네이션, 정렬) | USER 이상 |
| GET | `/{id}` | 단어 상세 조회 (isMemorized·isBookmarked 포함) | USER 이상 |
| GET | `/search` | 단어 검색 (영어·한글 부분 일치, 페이지네이션) | USER 이상 |
| POST | `/` | 단어 추가 (최대 50개 제한) | ADMIN |
| POST | `/bulk` | CSV 단어 일괄 추가 (최대 1000행, 헤더 검증) | ADMIN |
| PATCH | `/{id}` | 단어 수정 (null 아닌 필드만 반영) | ADMIN |
| DELETE | `/{id}` | 단어 삭제 (소프트 삭제) | ADMIN |

단어 목록·검색 쿼리 파라미터: `page`(기본 0), `size`(기본 20), `sort`(기본 `id,asc` / `english,asc` / `english,desc`)

### UserWordController (`/api/v1/user/words`)

| Method | Path | 설명 | 필요 권한 |
|--------|------|------|-----------|
| GET | `/bookmarked` | 북마크 단어 목록 조회 (페이지 없음, 최대 50개) | USER 이상 |
| GET | `/memorized` | 암기 완료 단어 목록 조회 (페이지 없음, 최대 50개) | USER 이상 |
| PATCH | `/{wordId}/bookmark` | 북마크 토글 | USER 이상 |
| PATCH | `/{wordId}/memorize` | 암기 상태 토글 | USER 이상 |

### ProgressController (`/api/v1/user/progress`)

| Method | Path | 설명 | 필요 권한 |
|--------|------|------|-----------|
| GET | `/` | 진도율·등급 조회 (레벨 1~5, 다음 레벨까지 남은 개수) | USER 이상 |

### TestSessionController (`/api/v1/test/sessions`)

| Method | Path | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/` | 테스트 세션 시작 (totalCount 1~50, quizType 선택) | O |
| POST | `/{sessionId}/answers` | 답안 제출 (대소문자 무시, 공백 제거) | O |
| POST | `/{sessionId}/finish` | 테스트 종료 (durationSec 저장, 정답률 계산) | O |
| GET | `/` | 테스트 기록 조회 (완료된 세션만, 최신순 페이지네이션) | O |
| GET | `/{id}` | 테스트 세션 상세 조회 (세션 소유자 확인) | O |

### AdminController (`/api/v1/admin`)

| Method | Path | 설명 | 필요 권한 |
|--------|------|------|-----------|
| GET | `/users` | USER 역할 사용자 목록 전체 조회 | ADMIN |
| GET | `/users/{userId}/test-sessions` | 특정 사용자 테스트 기록 (페이지네이션) | ADMIN |
| GET | `/test-sessions/{sessionId}` | 테스트 세션 상세 조회 (소유자 확인 없음) | ADMIN |

## 컨트롤러 공통 패턴

### 세션 인증 확인

모든 인증 필요 엔드포인트 상단에 아래 패턴이 반복된다:

```java
HttpSession session = httpRequest.getSession(false);
if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
}
Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
```

### ADMIN 권한 확인

```java
Object roleObj = session.getAttribute("LOGIN_USER_ROLE");
String role = roleObj != null ? roleObj.toString() : "";
if (!"ADMIN".equals(role)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiResponse<>(false, "관리자 접근 권한이 없습니다.", null));
}
```

### 예외 처리 원칙

비즈니스 예외(`BusinessException` 하위)는 서비스에서 throw → `GlobalExceptionHandler`가 자동으로 표준 응답으로 변환하므로 컨트롤러에서 try-catch 불필요.

단, 세션 검사 결과(401/403)는 컨트롤러에서 `ResponseEntity`로 직접 응답 — 핸들러를 거치지 않는다.

## 주요 응답 필드 참고

**단어 목록 (`data.content[]`):**
```
id, english, meaning, pronunciation, exampleSentence,
isMemorized, isBookmarked, createdAt, updatedAt
```

**테스트 시작 (`data`):**
```
sessionId, currentIndex(0), totalCount, isFinished(false),
quizType(SHORT_ANSWER|MULTIPLE_CHOICE|FILL_IN_BLANK),
currentQuestion: { wordId, english, meaning, pronunciation, exampleSentence, choices[] }
```
- `choices[]`: MULTIPLE_CHOICE·FILL_IN_BLANK 유형만 반환 (4지선다)
- FILL_IN_BLANK: `exampleSentence`에서 정답 단어를 `___`로 마스킹

**답안 제출 (`data`):**
```
sessionId, currentIndex(증가), totalCount, isCorrect, isFinished,
currentQuestion(isFinished=true이면 null)
```

**테스트 종료 (`data`):**
```
sessionId, totalCount, correctCount, accuracy(소수점 1자리),
durationSec, wrongAnswers[], finishedAt
```

**진도율 (`data`):**
```
level(1~5), totalWords, memorizedWords, wordsToNextLevel
```
레벨 기준: 0~9 → L1, 10~19 → L2, 20~29 → L3, 30~39 → L4, 40+ → L5
