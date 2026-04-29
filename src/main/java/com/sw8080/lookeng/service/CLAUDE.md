# CLAUDE.md — service/

## AuthService

- `signup` / `adminSignup`: 이메일 중복 검사 → BCrypt 암호화 저장. 차이는 `role` 값만 (`USER` vs `ADMIN`)
- `login`: 이메일로 유저 조회 → `passwordEncoder.matches()` 검증. 이메일/비밀번호 오류 모두 동일 메시지로 응답 (보안)

## WordService

- **단어 최대 50개 제한**: `createWord`에서 `wordRepository.count() >= 50` 시 `BadRequestException`
- 수정(`updateWord`): `word.update(...)` 호출 후 별도 save 없이 JPA Dirty Checking으로 반영
- `english` 필드 수정 시에만 중복 검사 (`existsByEnglishAndIdNot`)
- 목록 조회의 `isMemorized` / `isBookmarked`는 USER_WORD 테이블 미구현으로 현재 `false` 하드코딩

## TestSessionService

**세션 시작 (`startSession`)**
1. `totalCount` 유효성 검사 (1~50)
2. 전체 단어 조회 → `Collections.shuffle()` → 앞 N개 선택
3. `TestSession` 저장 후 첫 번째 문제 DTO 반환

**답안 제출 (`submitAnswer`)**
1. 세션 소유자 확인 (`userId` 비교) — 불일치 시 403
2. 정답 판정: `word.getEnglish().equalsIgnoreCase(userInput.trim())`
3. `TestAnswer` 저장 → `session.submitAnswer(isCorrect)` 호출 (currentIndex++, 정답이면 correctCount++)
4. `currentIndex >= totalCount`면 `isFinished = true`, 아니면 다음 문제 DTO 포함 반환

**테스트 종료 (`finishSession`)**
1. `durationSec` 저장
2. 정답률(accuracy): `Math.round((correctCount / totalCount) * 1000) / 10.0` (소수점 첫째 자리)
3. 오답 목록: `testAnswerRepository.findByTestSessionIdAndIsCorrectFalse(sessionId)`

**기록 조회 (`getTestHistory`)**
- `createdAt` 기준 내림차순 페이지네이션
- 정답률은 조회 시마다 재계산 (DB 저장 없음)
