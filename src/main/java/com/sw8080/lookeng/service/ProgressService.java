package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.response.ProgressResponseDto;
import com.sw8080.lookeng.repository.UserWordRepository;
import com.sw8080.lookeng.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;

    @Transactional(readOnly = true)
    public ProgressResponseDto getProgress(Long userId) {
        // 1. 전체 단어 수 조회
        long totalWords = wordRepository.count();

        // 2. 암기 완료 단어 수 조회 (isMemorized = true 기준)
        long memorizedWords = userWordRepository.countByUserIdAndIsMemorizedTrue(userId);

        // 3. 레벨 및 다음 레벨까지 남은 개수 계산
        int level = calculateLevel(memorizedWords);
        long wordsToNextLevel = calculateWordsToNextLevel(level, memorizedWords);

        // 4. 응답 DTO 변환
        return ProgressResponseDto.from(level, totalWords, memorizedWords, wordsToNextLevel);
    }

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

    private long calculateWordsToNextLevel(int currentLevel, long count) {
        if (currentLevel >= 5) {
            return 0;
        }
        return (currentLevel * 10L) - count;
    }
}
