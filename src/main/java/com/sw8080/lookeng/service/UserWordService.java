package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.response.BookmarkResponseDto;
import com.sw8080.lookeng.dto.response.BookmarkedWordDto;
import com.sw8080.lookeng.dto.response.MemorizeResponseDto;
import com.sw8080.lookeng.dto.response.MemorizedWordDto;
import com.sw8080.lookeng.entity.UserWord;
import com.sw8080.lookeng.entity.Word;
import com.sw8080.lookeng.exception.NotFoundException;
import com.sw8080.lookeng.repository.UserWordRepository;
import com.sw8080.lookeng.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserWordService {

    private final UserWordRepository userWordRepository;
    private final WordRepository wordRepository;

    @Transactional
    public BookmarkResponseDto toggleBookmark(Long userId, Long wordId) {
        // 1. 단어 존재 여부 확인
        wordRepository.findById(wordId)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. USER_WORD 레코드 조회
        Optional<UserWord> userWordOpt = userWordRepository.findByUserIdAndWordId(userId, wordId);

        boolean newBookmarkState;
        if (userWordOpt.isEmpty()) {
            // 3. 없으면 is_bookmarked = true로 신규 생성
            UserWord userWord = UserWord.builder()
                    .userId(userId)
                    .wordId(wordId)
                    .isBookmarked(true)
                    .isMemorized(false)
                    .memorizedAt(null)
                    .build();
            userWordRepository.save(userWord);
            newBookmarkState = true;
        } else {
            // 4. 있으면 is_bookmarked 반전 (Dirty Checking 발동)
            UserWord userWord = userWordOpt.get();
            userWord.toggleBookmark();
            newBookmarkState = userWord.isBookmarked();
        }

        // 5. 응답 DTO 반환
        return BookmarkResponseDto.from(wordId, newBookmarkState);
    }

    @Transactional
    public MemorizeResponseDto toggleMemorize(Long userId, Long wordId) {
        // 1. 단어 존재 여부 확인
        wordRepository.findById(wordId)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. USER_WORD 레코드 조회
        Optional<UserWord> userWordOpt = userWordRepository.findByUserIdAndWordId(userId, wordId);

        boolean newMemorizedState;
        LocalDateTime newMemorizedAt;
        if (userWordOpt.isEmpty()) {
            // 3. 없으면 is_memorized = true, memorized_at = NOW()로 신규 생성
            LocalDateTime now = LocalDateTime.now();
            UserWord userWord = UserWord.builder()
                    .userId(userId)
                    .wordId(wordId)
                    .isBookmarked(false)
                    .isMemorized(true)
                    .memorizedAt(now)
                    .build();
            userWordRepository.save(userWord);
            newMemorizedState = true;
            newMemorizedAt = now;
        } else {
            // 4. 있으면 is_memorized 반전, memorized_at 갱신 (Dirty Checking 발동)
            UserWord userWord = userWordOpt.get();
            userWord.toggleMemorize();
            newMemorizedState = userWord.isMemorized();
            newMemorizedAt = userWord.getMemorizedAt();
        }

        // 5. 응답 DTO 반환
        return MemorizeResponseDto.from(wordId, newMemorizedState, newMemorizedAt);
    }

    @Transactional(readOnly = true)
    public List<BookmarkedWordDto> getBookmarkedWords(Long userId) {
        // 1. 해당 유저의 북마크된 USER_WORD 레코드 조회
        List<UserWord> userWords = userWordRepository.findByUserIdAndIsBookmarkedTrue(userId);

        // 2. wordId 목록 추출 후 Word 일괄 조회 (@SQLRestriction으로 삭제된 단어 자동 제외)
        List<Long> wordIds = userWords.stream()
                .map(UserWord::getWordId)
                .collect(Collectors.toList());
        List<Word> words = wordRepository.findAllById(wordIds);

        // 3. wordId → UserWord 맵 생성 (isMemorized 참조용)
        Map<Long, UserWord> userWordMap = userWords.stream()
                .collect(Collectors.toMap(UserWord::getWordId, uw -> uw));

        // 4. DTO 변환 (삭제된 단어는 words 목록에서 이미 제외됨)
        return words.stream()
                .map(word -> BookmarkedWordDto.from(word, userWordMap.get(word.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemorizedWordDto> getMemorizedWords(Long userId) {
        // 1. 해당 유저의 암기 완료된 USER_WORD 레코드 조회
        List<UserWord> userWords = userWordRepository.findByUserIdAndIsMemorizedTrue(userId);

        // 2. wordId 목록 추출 후 Word 일괄 조회 (@SQLRestriction으로 삭제된 단어 자동 제외)
        List<Long> wordIds = userWords.stream()
                .map(UserWord::getWordId)
                .collect(Collectors.toList());
        List<Word> words = wordRepository.findAllById(wordIds);

        // 3. wordId → UserWord 맵 생성 (isBookmarked, memorizedAt 참조용)
        Map<Long, UserWord> userWordMap = userWords.stream()
                .collect(Collectors.toMap(UserWord::getWordId, uw -> uw));

        // 4. DTO 변환 (삭제된 단어는 words 목록에서 이미 제외됨)
        return words.stream()
                .map(word -> MemorizedWordDto.from(word, userWordMap.get(word.getId())))
                .collect(Collectors.toList());
    }
}
