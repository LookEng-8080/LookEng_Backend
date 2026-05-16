# CLAUDE.md — service/

## AuthService

- `signup` / `adminSignup`: 이메일 중복 검사 → BCrypt 암호화 저장. 차이는 `role` 값만 (`USER` vs `ADMIN`)
- `login`: 이메일로 유저 조회 → `passwordEncoder.matches()` 검증. 이메일/비밀번호 오류 모두 동일 메시지로 응답 (보안)

## WordService

- **단어 최대 50개 제한**: `createWord`에서 `wordRepository.count() >= 50` 시 `BadRequestException`
- 수정(`updateWord`): `word.update(...)` 호출 후 별도 save 없이 JPA Dirty Checking으로 반영
- `english` 필드 수정 시에만 중복 검사 (`existsByEnglishAndIdNot`)
- 목록 조회(`getWordList`): 페이지 wordId 목록으로 `userWordRepository.findByUserIdAndWordIdIn()` 일괄 조회 → 맵으로 변환 후 실제 `isMemorized` / `isBookmarked` 반영
- 상세 조회(`getWordDetail`): `userWordRepository.findByUserIdAndWordId()` 조회 → `Optional.map()` 으로 상태 추출, 레코드 없으면 `false`

## UserWordService

**북마크 단어 목록 조회 (`getBookmarkedWords`)**
1. `userWordRepository.findByUserIdAndIsBookmarkedTrue(userId)` → 북마크된 UserWord 목록
2. wordId 목록 추출 → `wordRepository.findAllById(wordIds)` 일괄 조회 (`@SQLRestriction`으로 삭제 단어 자동 제외)
3. `wordId → UserWord` 맵 생성 (isMemorized 참조용)
4. `BookmarkedWordDto.from(word, userWord)` 변환 후 반환 (페이지네이션 없음, 최대 50개 보장)

**암기 완료 단어 목록 조회 (`getMemorizedWords`)**
1. `userWordRepository.findByUserIdAndIsMemorizedTrue(userId)` → 암기 완료된 UserWord 목록
2. wordId 목록 추출 → `wordRepository.findAllById(wordIds)` 일괄 조회 (`@SQLRestriction`으로 삭제 단어 자동 제외)
3. `wordId → UserWord` 맵 생성 (isBookmarked, memorizedAt 참조용)
4. `MemorizedWordDto.from(word, userWord)` 변환 후 반환 (페이지네이션 없음, 최대 50개 보장)

**북마크 토글 (`toggleBookmark`)**
1. `wordRepository.findById(wordId)` — 존재하지 않으면 `NotFoundException` (404)
2. `userWordRepository.findByUserIdAndWordId(userId, wordId)` 조회
3. 레코드 없으면 → `isBookmarked = true`로 신규 생성 후 save
4. 레코드 있으면 → `userWord.toggleBookmark()` 호출 (Dirty Checking으로 반영)
5. `BookmarkResponseDto.from(wordId, newState)` 반환

**암기 상태 토글 (`toggleMemorize`)**
1. `wordRepository.findById(wordId)` — 존재하지 않으면 `NotFoundException` (404)
2. `userWordRepository.findByUserIdAndWordId(userId, wordId)` 조회
3. 레코드 없으면 → `isMemorized = true`, `memorizedAt = NOW()`로 신규 생성 후 save
4. 레코드 있으면 → `userWord.toggleMemorize()` 호출 (Dirty Checking으로 반영)
   - `true → false`: `memorizedAt = null`
   - `false → true`: `memorizedAt = LocalDateTime.now()`
5. `MemorizeResponseDto.from(wordId, newState, memorizedAt)` 반환

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
