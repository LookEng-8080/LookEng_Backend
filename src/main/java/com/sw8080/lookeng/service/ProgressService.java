package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.response.ProgressResponseDto;
import com.sw8080.lookeng.repository.TestAnswerRepository;
import com.sw8080.lookeng.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final WordRepository wordRepository;
    private final TestAnswerRepository testAnswerRepository;

    @Transactional(readOnly = true)
    public ProgressResponseDto getProgress(Long userId) {
        // 1. 전체 단어 수 조회
        long totalWords = wordRepository.count();

        // 2. 학습 완료 단어 수 조회 (퀴즈에서 정답을 맞힌 고유 단어 개수)
        long masteredWords = testAnswerRepository.countDistinctCorrectWordsByUserId(userId);

        // 3. 레벨 및 다음 레벨까지 남은 개수 계산
        int level = calculateLevel(masteredWords);
        long wordsToNextLevel = calculateWordsToNextLevel(level, masteredWords);

        // 4. 응답 DTO 변환
        return ProgressResponseDto.from(level, totalWords, masteredWords, wordsToNextLevel);
    }

    // 5. 레벨 계산 로직
    private int calculateLevel(long count) {
        if (count >= 40) {
            return 5;
        }
        if (count >= 30) {
            return 4;
        }
        if (count >= 20) {
            return 3;
        }
        if (count >= 10) {
            return 2;
        }
        return 1;
    }

    // 6. 다음 레벨까지 남은 단어 수 계산 로직
    private long calculateWordsToNextLevel(int currentLevel, long count) {
        if (currentLevel >= 5) {
            return 0;
        }
        return (currentLevel * 10L) - count;
    }
}
