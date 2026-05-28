# CLAUDE.md — service/

## AuthService

- `signup`: 이메일 중복 검사 → BCrypt 암호화 → `Role.USER`로 저장
- `adminSignup`: 이메일 중복 검사 + `lookeng.admin.secret-code` 검증 → `Role.ADMIN`으로 저장
- `login`: 이메일로 유저 조회 → `passwordEncoder.matches()` 검증. 이메일/비밀번호 오류 모두 동일 메시지로 응답 (보안)
- `logout`: 세션 무효화 처리 (컨트롤러에서 직접 `session.invalidate()`)
- `withdraw`: `Role.USER`만 허용 (ADMIN이면 403). 소프트 삭제 (`deletedAt` 설정)
- `requestPasswordReset`: 5자리 랜덤 토큰 생성 → `PasswordResetToken`으로 저장 (3분 유효) → 이메일 발송
- `resetPassword`: 토큰 조회 → 만료 여부 확인 → BCrypt 암호화 후 비밀번호 업데이트 → 토큰 삭제
- `socialLogin`: Google ID Token → `GoogleIdTokenVerifier`로 검증 → 이메일 조회 후 기존 회원이면 로그인, 없으면 자동 가입 (passwordHash = null)

## WordService

- **단어 최대 50개 제한**: `createWord`에서 `wordRepository.count() >= 50` 시 `BadRequestException`
- **소프트 삭제 단어 복구**: `createWord` 및 `bulkUpload` 시 이미 삭제된(`deletedAt != null`) 동일 영어 단어가 있으면 복구 처리
- `updateWord`: `word.update(...)` 호출 후 별도 save 없이 JPA Dirty Checking으로 반영. `english` 필드 수정 시에만 중복 검사 (`existsByEnglishAndIdNot`)
- `deleteWord`: 소프트 삭제 (`@SQLDelete` 적용)
- `getWordList`: 페이지 wordId 목록으로 `userWordRepository.findByUserIdAndWordIdIn()` 일괄 조회 → 맵으로 변환 후 실제 `isMemorized` / `isBookmarked` 반영
- `getWordDetail`: `userWordRepository.findByUserIdAndWordId()` 조회 → `Optional.map()`으로 상태 추출, 레코드 없으면 `false`
- `searchWords`: 영어·한글 부분 일치 (`LIKE %keyword%`) 페이지 검색 → UserWord 일괄 조회로 isMemorized/isBookmarked 반영
- `bulkUpload`: CSV MultipartFile 파싱 (헤더 검증: `english,meaning,partOfSpeech,exampleSentence`), 최대 1000행 제한. 삭제된 단어 복구, 중복 단어 스킵

## UserWordService

**북마크 단어 목록 조회 (`getBookmarkedWords`)**
1. `userWordRepository.findByUserIdAndIsBookmarkedTrue(userId)` → 북마크된 UserWord 목록
2. wordId 목록 추출 → `wordRepository.findAllById(wordIds)` 일괄 조회 (`@SQLRestriction`으로 삭제 단어 자동 제외)
3. `wordId → UserWord` 맵 생성 (isMemorized 참조용)
4. `BookmarkedWordDto.from(word, userWord)` 변환 후 반환 (페이지네이션 없음, 최대 50개 보장)

**암기 완료 단어 목록 조회 (`getMemorizedWords`)** — 동일 패턴, `isMemorizedTrue` 조건

**북마크 토글 (`toggleBookmark`)**
1. `wordRepository.findById(wordId)` — 없으면 `NotFoundException`
2. `userWordRepository.findByUserIdAndWordId()` 조회
3. 레코드 없으면 → `isBookmarked = true`로 신규 생성 후 save
4. 레코드 있으면 → `userWord.toggleBookmark()` (Dirty Checking)
5. `BookmarkResponseDto.from(wordId, newState)` 반환

**암기 상태 토글 (`toggleMemorize`)**
1. `wordRepository.findById(wordId)` — 없으면 `NotFoundException`
2. `userWordRepository.findByUserIdAndWordId()` 조회
3. 레코드 없으면 → `isMemorized = true`, `memorizedAt = NOW()`로 신규 생성 후 save
4. 레코드 있으면 → `userWord.toggleMemorize()` 호출
   - `true → false`: `memorizedAt = null`
   - `false → true`: `memorizedAt = LocalDateTime.now()`
5. `MemorizeResponseDto.from(wordId, newState, memorizedAt)` 반환

## TestSessionService

**세션 시작 (`startSession`)**
1. `totalCount` 유효성 검사 (1~50, 초과 시 `BadRequestException`)
2. 전체 단어 조회 → `Collections.shuffle()` → 앞 N개 선택
3. `TestSession` 저장 후 첫 번째 문제 DTO 반환
   - `SHORT_ANSWER`: 그대로 반환
   - `MULTIPLE_CHOICE` / `FILL_IN_BLANK`: 오답 보기 3개 추가 (전체 단어에서 랜덤 선택, `choices[]`)
   - `FILL_IN_BLANK`: `exampleSentence`에서 정답 영어 단어를 `___`로 마스킹

**답안 제출 (`submitAnswer`)**
1. 세션 소유자 확인 (`userId` 비교) — 불일치 시 `ForbiddenException`
2. 정답 판정: `word.getEnglish().equalsIgnoreCase(userInput.trim())`
3. `TestAnswer` 저장 → `session.submitAnswer(isCorrect)` 호출 (`currentIndex++`, 정답이면 `correctCount++`)
4. `currentIndex >= totalCount`면 `isFinished = true`, 아니면 다음 문제 DTO 포함 반환

**테스트 종료 (`finishSession`)**
1. `durationSec` 저장 (`session.finishTest(durationSec)`)
2. 정답률 계산: `Math.round((correctCount / totalCount) * 1000) / 10.0` (소수점 첫째 자리)
3. 오답 목록: `testAnswerRepository.findByTestSessionIdAndIsCorrectFalse(sessionId)`

**기록 조회 (`getTestHistory`)**
- `isFinished = true`인 세션만 조회 (`createdAt` 기준 내림차순 페이지네이션)
- 정답률은 조회 시마다 재계산 (DB 저장 없음)

**세션 상세 조회 (`getSessionDetail` / `getSessionDetailForAdmin`)**
- `getSessionDetail(userId, sessionId)`: 세션 소유자 확인 후 답안 목록 반환
- `getSessionDetailForAdmin(sessionId)`: 소유자 확인 없이 상세 조회 (AdminController 전용)

## ProgressService

암기 완료 수 (`UserWord.isMemorized = true` 카운트) 기반으로 레벨 계산:

| 암기 단어 수 | 레벨 |
|------------|------|
| 0 ~ 9      | 1    |
| 10 ~ 19    | 2    |
| 20 ~ 29    | 3    |
| 30 ~ 39    | 4    |
| 40+        | 5    |

- `wordsToNextLevel`: `currentLevel * 10 - memorizedWords` (레벨 5면 0)
- 전체 단어 수(`totalWords`)는 `wordRepository.count()` 조회

## AdminService

- `getUsers()`: `userRepository.findAllByRole(Role.USER)` → `UserListResponseDto` 변환
  - ADMIN 역할 사용자는 제외 (역할별 필터링)
  - 전체 조회 (페이지네이션 없음)
