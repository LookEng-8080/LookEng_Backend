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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TestSessionService {

    private final TestSessionRepository testSessionRepository;
    private final WordRepository wordRepository;
    private final TestAnswerRepository testAnswerRepository;

    public TestSessionResponseDto startSession(Long userId, TestSessionRequestDto request) {
        // 1. totalCount 유효성 검사
        if (request.getTotalCount() <= 0 || request.getTotalCount() > 50) {
            throw new BadRequestException("개수가 잘못되었습니다. (1~50 사이로 입력해주세요)");
        }

        List<Word> allWords = wordRepository.findAll();

        // 2. FILL_IN_BLANK: 예문 있는 단어만 출제 가능
        List<Word> candidateWords;
        if (request.getQuizType() == com.sw8080.lookeng.entity.QuizType.FILL_IN_BLANK) {
            candidateWords = allWords.stream()
                    .filter(w -> w.getExampleSentence() != null && !w.getExampleSentence().isBlank())
                    .collect(Collectors.toList());
            if (candidateWords.size() < request.getTotalCount()) {
                throw new BadRequestException("예문이 있는 단어 수가 요청한 수보다 적습니다.");
            }
        } else {
            candidateWords = allWords;
            if (candidateWords.size() < request.getTotalCount()) {
                throw new BadRequestException("전체 단어 수가 요청한 수보다 적습니다.");
            }
        }

        // 3. 랜덤 추출
        Collections.shuffle(candidateWords);
        List<Word> selectedWords = new ArrayList<>(candidateWords.subList(0, request.getTotalCount()));

        // 4. 세션 생성 및 저장
        TestSession session = TestSession.builder()
                .userId(userId)
                .quizType(request.getQuizType())
                .totalCount(request.getTotalCount())
                .currentIndex(0)
                .words(selectedWords)
                .build();

        testSessionRepository.save(session);

        // 5. 첫 번째 문제 조립
        Word firstWord = selectedWords.get(0);
        return TestSessionResponseDto.builder()
                .sessionId(session.getId())
                .quizType(session.getQuizType())
                .totalCount(session.getTotalCount())
                .currentIndex(0)
                .question(buildQuestionDto(firstWord, session.getQuizType(), selectedWords))
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

        // 2. 완료된 세션 재제출 차단
        if (session.isAllAnswered()) {
            throw new BadRequestException("이미 완료된 테스트 세션입니다.");
        }

        // 3. 현재 문제 단어와 wordId 일치 검증
        Word currentWord = session.getWords().get(session.getCurrentIndex());
        if (!currentWord.getId().equals(request.getWordId())) {
            throw new BadRequestException("현재 문제의 단어 ID와 일치하지 않습니다.");
        }

        Word word = currentWord;

        // 4. 정답 판정 (대소문자 무시, 앞뒤 공백 제거)
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
            nextQuestion = buildQuestionDto(nextWord, session.getQuizType(), session.getWords());
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

        if (session.isFinished()) {
            throw new BadRequestException("이미 종료된 테스트 세션입니다.");
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
    private TestSessionResponseDto.QuestionDto buildQuestionDto(
            Word word, com.sw8080.lookeng.entity.QuizType quizType, List<Word> pool) {

        return switch (quizType) {
            case MULTIPLE_CHOICE -> TestSessionResponseDto.QuestionDto.builder()
                    .wordId(word.getId())
                    .korean(word.getKorean())
                    .partOfSpeech(word.getPartOfSpeech())
                    .choices(generateChoices(word, pool))
                    .build();
            case FILL_IN_BLANK -> TestSessionResponseDto.QuestionDto.builder()
                    .wordId(word.getId())
                    .sentence(blankSentence(word.getEnglish(), word.getExampleSentence()))
                    .choices(generateChoices(word, pool))
                    .build();
            default -> TestSessionResponseDto.QuestionDto.builder()
                    .wordId(word.getId())
                    .korean(word.getKorean())
                    .partOfSpeech(word.getPartOfSpeech())
                    .exampleSentence(word.getExampleSentence())
                    .build();
        };
    }

    private List<String> generateChoices(Word correct, List<Word> pool) {
        // 1. 풀에서 정답 제외한 오답 후보 목록 구성
        List<String> distractors = pool.stream()
                .filter(w -> !w.getId().equals(correct.getId()))
                .map(Word::getEnglish)
                .collect(Collectors.toList());
        Collections.shuffle(distractors);

        // 2. 오답이 3개 미만이면 DB 전체에서 보충
        if (distractors.size() < 3) {
            List<String> allEnglish = wordRepository.findAll().stream()
                    .filter(w -> !w.getId().equals(correct.getId()))
                    .map(Word::getEnglish)
                    .collect(Collectors.toList());
            Collections.shuffle(allEnglish);
            for (String e : allEnglish) {
                if (!distractors.contains(e)) {
                    distractors.add(e);
                }
                if (distractors.size() == 3) break;
            }
        }

        // 3. 정답 포함 4지선다 조합 후 셔플
        List<String> choices = new ArrayList<>(distractors.subList(0, Math.min(3, distractors.size())));
        choices.add(correct.getEnglish());
        Collections.shuffle(choices);
        return choices;
    }

    private String blankSentence(String english, String exampleSentence) {
        return exampleSentence.replaceAll(
                "(?i)\\b" + Pattern.quote(english) + "\\b", "_____");
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
