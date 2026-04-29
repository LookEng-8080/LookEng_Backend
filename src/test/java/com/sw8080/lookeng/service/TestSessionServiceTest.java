package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.request.TestAnswerRequestDto;
import com.sw8080.lookeng.dto.request.TestFinishRequestDto;
import com.sw8080.lookeng.dto.request.TestSessionRequestDto;
import com.sw8080.lookeng.dto.response.TestAnswerResponseDto;
import com.sw8080.lookeng.dto.response.TestFinishResponseDto;
import com.sw8080.lookeng.dto.response.TestHistoryResponseDto;
import com.sw8080.lookeng.dto.response.TestSessionResponseDto;
import com.sw8080.lookeng.entity.QuizType;
import com.sw8080.lookeng.entity.TestAnswer;
import com.sw8080.lookeng.entity.TestSession;
import com.sw8080.lookeng.entity.Word;
import com.sw8080.lookeng.exception.BadRequestException;
import com.sw8080.lookeng.exception.ForbiddenException;
import com.sw8080.lookeng.exception.NotFoundException;
import com.sw8080.lookeng.repository.TestAnswerRepository;
import com.sw8080.lookeng.repository.TestSessionRepository;
import com.sw8080.lookeng.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestSessionServiceTest {

    @Mock private TestSessionRepository testSessionRepository;
    @Mock private WordRepository wordRepository;
    @Mock private TestAnswerRepository testAnswerRepository;

    @InjectMocks
    private TestSessionService testSessionService;

    private Word word1;
    private Word word2;
    private Word word3;

    @BeforeEach
    void setUp() {
        word1 = Word.builder().english("apple").korean("사과").partOfSpeech("noun").exampleSentence("").pronunciationUrl(null).build();
        word2 = Word.builder().english("banana").korean("바나나").partOfSpeech("noun").exampleSentence("").pronunciationUrl(null).build();
        word3 = Word.builder().english("cat").korean("고양이").partOfSpeech("noun").exampleSentence("").pronunciationUrl(null).build();
    }

    // ── startSession ────────────────────────────────────────────

    @Test
    @DisplayName("세션 시작 성공 - 첫 번째 문제 반환")
    void startSession_success() {
        TestSessionRequestDto request = new TestSessionRequestDto(QuizType.SHORT_ANSWER, 2);
        given(wordRepository.findAll()).willReturn(new ArrayList<>(List.of(word1, word2, word3)));
        given(testSessionRepository.save(any(TestSession.class))).willAnswer(inv -> inv.getArgument(0));

        TestSessionResponseDto result = testSessionService.startSession(1L, request);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getCurrentIndex()).isEqualTo(0);
        assertThat(result.getQuestion()).isNotNull();
    }

    @Test
    @DisplayName("세션 시작 실패 - 단어 수 부족 → BadRequestException")
    void startSession_notEnoughWords() {
        TestSessionRequestDto request = new TestSessionRequestDto(QuizType.SHORT_ANSWER, 5);
        given(wordRepository.findAll()).willReturn(List.of(word1, word2));

        assertThatThrownBy(() -> testSessionService.startSession(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    // ── submitAnswer ─────────────────────────────────────────────

    @Test
    @DisplayName("답안 제출 성공 - 정답")
    void submitAnswer_correct() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(1L, "apple");
        TestSession session = buildSession(1L, List.of(word1, word2), 0);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word1));
        given(testAnswerRepository.save(any(TestAnswer.class))).willAnswer(inv -> inv.getArgument(0));

        TestAnswerResponseDto result = testSessionService.submitAnswer(1L, 1L, request);

        assertThat(result.isCorrect()).isTrue();
        assertThat(result.isFinished()).isFalse();
    }

    @Test
    @DisplayName("답안 제출 성공 - 오답")
    void submitAnswer_wrong() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(1L, "wronganswer");
        TestSession session = buildSession(1L, List.of(word1, word2), 0);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word1));
        given(testAnswerRepository.save(any(TestAnswer.class))).willAnswer(inv -> inv.getArgument(0));

        TestAnswerResponseDto result = testSessionService.submitAnswer(1L, 1L, request);

        assertThat(result.isCorrect()).isFalse();
    }

    @Test
    @DisplayName("마지막 문제 정답 제출 - isFinished=true")
    void submitAnswer_lastQuestion_isFinished() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(2L, "banana");
        TestSession session = buildSession(1L, List.of(word1, word2), 1);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word2));
        given(testAnswerRepository.save(any(TestAnswer.class))).willAnswer(inv -> inv.getArgument(0));

        TestAnswerResponseDto result = testSessionService.submitAnswer(1L, 1L, request);

        assertThat(result.isFinished()).isTrue();
        assertThat(result.getNextQuestion()).isNull();
    }

    @Test
    @DisplayName("답안 제출 실패 - 이미 완료된 세션 → BadRequestException")
    void submitAnswer_alreadyFinished() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(1L, "apple");
        TestSession session = buildSession(1L, List.of(word1), 1); // currentIndex == totalCount
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> testSessionService.submitAnswer(1L, 1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("답안 제출 실패 - 타인 세션 → ForbiddenException")
    void submitAnswer_otherUsersSession() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(1L, "apple");
        TestSession session = buildSession(99L, List.of(word1, word2), 0); // userId=99
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> testSessionService.submitAnswer(1L, 1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("답안 제출 실패 - 현재 문제와 다른 wordId → BadRequestException")
    void submitAnswer_wrongWordId() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(999L, "apple"); // 잘못된 wordId
        TestSession session = buildSession(1L, List.of(word1, word2), 0);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> testSessionService.submitAnswer(1L, 1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("답안 제출 실패 - 없는 세션 → NotFoundException")
    void submitAnswer_sessionNotFound() {
        TestAnswerRequestDto request = new TestAnswerRequestDto(1L, "apple");
        given(testSessionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> testSessionService.submitAnswer(1L, 999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ── finishSession ────────────────────────────────────────────

    @Test
    @DisplayName("테스트 종료 성공 - 정답률 계산")
    void finishSession_success() {
        TestFinishRequestDto request = new TestFinishRequestDto(120);
        TestSession session = buildSession(1L, List.of(word1, word2), 2);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(testAnswerRepository.findByTestSessionIdAndIsCorrectFalse(anyLong())).willReturn(List.of());

        TestFinishResponseDto result = testSessionService.finishSession(1L, 1L, request);

        assertThat(result.getDurationSec()).isEqualTo(120);
    }

    @Test
    @DisplayName("테스트 종료 실패 - 이미 종료된 세션 → BadRequestException")
    void finishSession_alreadyFinished() {
        TestFinishRequestDto request = new TestFinishRequestDto(120);
        TestSession session = buildFinishedSession(1L, List.of(word1));
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> testSessionService.finishSession(1L, 1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("테스트 종료 실패 - durationSec=0 → BadRequestException")
    void finishSession_invalidDuration() {
        TestFinishRequestDto request = new TestFinishRequestDto(0);
        TestSession session = buildSession(1L, List.of(word1), 1);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> testSessionService.finishSession(1L, 1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("테스트 종료 실패 - 타인 세션 → ForbiddenException")
    void finishSession_otherUsersSession() {
        TestFinishRequestDto request = new TestFinishRequestDto(120);
        TestSession session = buildSession(99L, List.of(word1), 1);
        given(testSessionRepository.findById(1L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> testSessionService.finishSession(1L, 1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── getTestHistory ────────────────────────────────────────────

    @Test
    @DisplayName("테스트 기록 조회 성공 - 페이지네이션 + 정답률 계산")
    void getTestHistory_success() {
        TestSession session = buildSession(1L, List.of(word1, word2), 2);
        Page<TestSession> page = new PageImpl<>(List.of(session));
        given(testSessionRepository.findByUserId(anyLong(), any(Pageable.class))).willReturn(page);

        TestHistoryResponseDto result = testSessionService.getTestHistory(1L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────

    private TestSession buildSession(Long userId, List<Word> words, int currentIndex) {
        return TestSession.builder()
                .userId(userId)
                .quizType(QuizType.SHORT_ANSWER)
                .totalCount(words.size())
                .currentIndex(currentIndex)
                .correctCount(0)
                .durationSec(0)
                .words(new ArrayList<>(words))
                .build();
    }

    private TestSession buildFinishedSession(Long userId, List<Word> words) {
        TestSession session = buildSession(userId, words, words.size());
        session.finishTest(60);
        return session;
    }
}
