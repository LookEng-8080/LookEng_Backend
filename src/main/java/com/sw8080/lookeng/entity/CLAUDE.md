# CLAUDE.md — entity/

## 엔티티 개요

### User
- 테이블: `USER`
- **소프트 삭제**: `@SQLDelete`로 `deleted_at = NOW()` 업데이트, `@SQLRestriction("deleted_at IS NULL")`으로 삭제된 유저 자동 제외
- `passwordHash`: 소셜 로그인 시 NULL 허용
- `role`: `Role.USER` (기본값) 또는 `Role.ADMIN`

### Word
- `english`: UNIQUE 제약, 전체 최대 50개 제한 (서비스 레이어에서 관리)
- `update(...)` 메서드: null이 아닌 필드만 선택적으로 수정 (PATCH 동작)
- `@EntityListeners(AuditingEntityListener.class)`: `createdAt` / `updatedAt` 자동 기록

### TestSession (`BaseTimeEntity` 상속)
- `userId`: User 엔티티와 외래키 관계 없이 `Long`으로 보관 (조인 없이 세션 소유자 확인용)
- `words`: `@ManyToMany` → `test_session_words` 조인 테이블
- `submitAnswer(boolean)`: `correctCount` 증가 + `currentIndex++`
- `finishTest(int durationSec)`: 소요 시간 저장
- `quizType`: `QuizType.SHORT_ANSWER` 또는 `MULTIPLE_CHOICE`

### TestAnswer
- `testSession`과 `word`를 `@ManyToOne`으로 참조
- `isCorrect` 필드로 오답 목록 필터링에 사용

### BaseTimeEntity
- `createdAt`, `updatedAt` 공통 필드 제공 — `TestSession`이 상속
