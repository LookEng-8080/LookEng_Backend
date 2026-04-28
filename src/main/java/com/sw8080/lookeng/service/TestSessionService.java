package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.TestHistoryItemDto;
import com.sw8080.lookeng.exception.BadRequestException;
import com.sw8080.lookeng.exception.ForbiddenException;
import com.sw8080.lookeng.exception.NotFoundException;
import com.sw8080.lookeng.dto.request.TestAnswerRequestDto;
import com.sw8080.lookeng.dto.request.TestFinishRequestDto;
import com.sw8080.lookeng.dto.request.TestSessionRequestDto;
import com.sw8080.lookeng.dto.response.TestAnswerResponseDto;
import com.sw8080.lookeng.dto.response.TestFinishResponseDto;
import com.sw8080.lookeng.dto.response.TestHistoryResponseDto;
import com.sw8080.lookeng.dto.response.TestSessionResponseDto;
import com.sw8080.lookeng.entity.TestAnswer;
import com.sw8080.lookeng.entity.TestSession;
import com.sw8080.lookeng.entity.Word;
import com.sw8080.lookeng.repository.TestAnswerRepository;
import com.sw8080.lookeng.repository.TestSessionRepository;
import com.sw8080.lookeng.repository.WordRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestSessionService {

    private final TestSessionRepository testSessionRepository;
    private final WordRepository wordRepository;
    private final TestAnswerRepository testAnswerRepository;

    public TestSessionResponseDto startSession(Long userId, TestSessionRequestDto request) {
        // 1. 전체 단어 중 랜덤으로 n개 추출
        if (request.getTotalCount() <= 0 || request.getTotalCount() > 50) {
            throw new BadRequestException("개수가 잘못되었습니다. (1~50 사이로 입력해주세요)");
        }

        List<Word> allWords = wordRepository.findAll();
        if (allWords.size() < request.getTotalCount()) {
            throw new BadRequestException("전체 단어 수가 요청한 수보다 적습니다.");
        }

        Collections.shuffle(allWords);
        List<Word> selectedWords = allWords.subList(0, request.getTotalCount());

        // 2. 세션 생성 및 저장
        TestSession session = TestSession.builder()
                .userId(userId)
                .quizType(request.getQuizType())
                .totalCount(request.getTotalCount())
                .currentIndex(0)
                .words(selectedWords)
                .build();

        testSessionRepository.save(session);

        // 3. 첫 번째 문제 정보 조립
        Word firstWord = selectedWords.get(0);
        return TestSessionResponseDto.builder()
                .sessionId(session.getId())
                .quizType(session.getQuizType())
                .totalCount(session.getTotalCount())
                .currentIndex(0)
                .question(TestSessionResponseDto.QuestionDto.builder()
                        .wordId(firstWord.getId())
                        .korean(firstWord.getKorean())
                        .partOfSpeech(firstWord.getPartOfSpeech())
                        .exampleSentence(firstWord.getExampleSentence())
                        .build())
                .build();
    }
    @Transactional
    public TestAnswerResponseDto submitAnswer(Long userId, Long sessionId, TestAnswerRequestDto request) {

        // 1. 세션 조회 및 권한 검사 (명세서 403, 404 에러 처리)
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 세션입니다."));

        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 테스트 세션에만 접근할 수 있습니다.");
        }

        // 2. 단어 조회
        Word word = wordRepository.findById(request.getWordId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 단어입니다."));

        // 3. 정답 판정 (대소문자 무시, 앞뒤 공백 제거)
        String trimmedInput = request.getUserInput() != null ? request.getUserInput().trim() : "";
        boolean isCorrect = word.getEnglish().equalsIgnoreCase(trimmedInput);

        // 4. TestAnswer 기록 저장
        TestAnswer testAnswer = TestAnswer.builder()
                .testSession(session)
                .word(word)
                .userInput(trimmedInput)
                .isCorrect(isCorrect)
                .build();
        testAnswerRepository.save(testAnswer);

        // 5. 세션 상태 업데이트 (인덱스 증가, 정답수 증가)
        session.submitAnswer(isCorrect);

        // 6. 종료 여부 확인 및 다음 문제 준비
        boolean isFinished = session.getCurrentIndex() >= session.getTotalCount();
        TestSessionResponseDto.QuestionDto nextQuestion = null;

        if (!isFinished) {
            Word nextWord = session.getWords().get(session.getCurrentIndex());
            nextQuestion = TestSessionResponseDto.QuestionDto.builder()
                    .wordId(nextWord.getId())
                    .korean(nextWord.getKorean())
                    .partOfSpeech(nextWord.getPartOfSpeech())
                    .exampleSentence(nextWord.getExampleSentence())
                    .build();
        }

        // 7. 응답 조립
        return TestAnswerResponseDto.builder()
                .wordId(word.getId())
                .userInput(trimmedInput)
                .correctAnswer(word.getEnglish())
                .isCorrect(isCorrect)
                .currentCorrectCount(session.getCorrectCount())
                .answeredCount(session.getCurrentIndex())
                .totalCount(session.getTotalCount())
                .isFinished(isFinished)
                .nextQuestion(nextQuestion)
                .build();
    }

    @Transactional
    public TestFinishResponseDto finishSession(Long userId, Long sessionId, TestFinishRequestDto request) {
        // 1. 세션 조회 및 권한/유효성 검사
        TestSession session = testSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 세션입니다."));

        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 테스트 세션에만 접근할 수 있습니다.");
        }
        if (request.getDurationSec() == null || request.getDurationSec() <= 0) {
            throw new BadRequestException("올바른 소요 시간을 입력해주세요.");
        }

        // 2. 세션 종료 정보 업데이트
        session.finishTest(request.getDurationSec());

        // 3. 정답률(accuracy) 계산 (소수점 첫째 자리까지)
        double accuracy = 0.0;
        if (session.getTotalCount() > 0) {
            accuracy = Math.round(((double) session.getCorrectCount() / session.getTotalCount()) * 1000) / 10.0;
        }

        // 4. 오답 목록(wrongWords) 조회 및 DTO 변환
        List<TestAnswer> wrongAnswers = testAnswerRepository.findByTestSessionIdAndIsCorrectFalse(sessionId);

        List<TestFinishResponseDto.WrongWordDto> wrongWords = wrongAnswers.stream()
                .map(answer -> TestFinishResponseDto.WrongWordDto.builder()
                        .wordId(answer.getWord().getId())
                        .english(answer.getWord().getEnglish()) // 정답
                        .korean(answer.getWord().getKorean())   // 뜻
                        .userInput(answer.getUserInput())       // 내가 쓴 오답
                        .build())
                .toList();

        // 5. 최종 결과 반환
        return TestFinishResponseDto.builder()
                .sessionId(session.getId())
                .quizType(session.getQuizType())
                .totalCount(session.getTotalCount())
                .correctCount(session.getCorrectCount())
                .accuracy(accuracy)
                .durationSec(session.getDurationSec())
                .startedAt(session.getCreatedAt()) // BaseTimeEntity에서 상속받은 생성일
                .wrongWords(wrongWords)
                .build();
    }
    @Transactional(readOnly = true)
    public TestHistoryResponseDto getTestHistory(Long userId, int page, int size) {

        // 1. 페이지네이션 및 정렬 설정: "생성일(createdAt) 기준 내림차순(최신순)"
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 2. DB에서 유저의 테스트 세션 목록 조회
        Page<TestSession> sessionPage = testSessionRepository.findByUserId(userId, pageable);

        // 3. Entity -> DTO 변환 및 정답률(accuracy) 계산
        List<TestHistoryItemDto> content = sessionPage.getContent().stream()
                .map(session -> {
                    double accuracy = 0.0;
                    if (session.getTotalCount() > 0) {
                        accuracy = Math.round(((double) session.getCorrectCount() / session.getTotalCount()) * 1000) / 10.0;
                    }
                    return TestHistoryItemDto.builder()
                            .sessionId(session.getId())
                            .quizType(session.getQuizType())
                            .totalCount(session.getTotalCount())
                            .correctCount(session.getCorrectCount())
                            .accuracy(accuracy)
                            .durationSec(session.getDurationSec())
                            .startedAt(session.getCreatedAt()) // 테스트 시작 시간 = 세션 생성 시간
                            .build();
                })
                .toList();

        // 4. 페이지 정보와 함께 최종 응답 조립
        return TestHistoryResponseDto.builder()
                .content(content)
                .totalElements(sessionPage.getTotalElements())
                .totalPages(sessionPage.getTotalPages())
                .currentPage(sessionPage.getNumber())
                .size(sessionPage.getSize())
                .build();
    }
}
