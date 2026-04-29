# CLAUDE.md — exception/

## 예외 계층 구조

```
RuntimeException
└── BusinessException(message, HttpStatus)
    ├── BadRequestException      → 400
    ├── UnauthorizedException    → 401
    ├── ForbiddenException       → 403
    ├── NotFoundException        → 404
    └── DuplicateException       → 409
```

## 사용 방법

서비스 레이어에서 상황에 맞는 하위 예외를 throw하면 `GlobalExceptionHandler`가 자동으로 표준 응답 형식으로 변환한다:

```java
// 서비스에서
throw new NotFoundException("해당 단어를 찾을 수 없습니다.");

// GlobalExceptionHandler가 변환 →
// HTTP 404, { "success": false, "message": "해당 단어를 찾을 수 없습니다.", "data": null }
```

## GlobalExceptionHandler 처리 범위

| 예외 타입 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| `BusinessException` (및 하위) | 예외의 `status` 값 | 서비스 비즈니스 예외 |
| `MethodArgumentNotValidException` | 400 | Bean Validation 실패 시 첫 번째 오류 메시지 반환 |

컨트롤러에서 직접 `ResponseEntity`로 응답하는 401/403(세션 검사 결과)은 핸들러를 거치지 않는다.
