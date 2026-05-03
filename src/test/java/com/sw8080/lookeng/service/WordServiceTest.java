package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.request.WordCreateRequestDto;
import com.sw8080.lookeng.dto.response.WordDetailResponseDto;
import com.sw8080.lookeng.dto.response.WordListResponseDto;
import com.sw8080.lookeng.dto.response.WordResponseDto;
import com.sw8080.lookeng.dto.response.WordUpdateRequestDto;
import com.sw8080.lookeng.entity.Word;
import com.sw8080.lookeng.exception.BadRequestException;
import com.sw8080.lookeng.exception.DuplicateException;
import com.sw8080.lookeng.exception.NotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @InjectMocks
    private WordService wordService;

    private Word word;

    @BeforeEach
    void setUp() {
        word = Word.builder()
                .english("apple")
                .korean("사과")
                .partOfSpeech("noun")
                .exampleSentence("I ate an apple.")
                .pronunciationUrl(null)
                .build();
    }

    @Test
    @DisplayName("단어 추가 성공")
    void createWord_success() {
        WordCreateRequestDto request = new WordCreateRequestDto("apple", "사과", "noun", "I ate an apple.", null);

        given(wordRepository.count()).willReturn(0L);
        given(wordRepository.existsByEnglishAndDeletedFalse(anyString())).willReturn(false);
        given(wordRepository.save(any(Word.class))).willReturn(word);

        WordResponseDto result = wordService.createWord(request);

        assertThat(result.getEnglish()).isEqualTo("apple");
        assertThat(result.getKorean()).isEqualTo("사과");
    }

    @Test
    @DisplayName("단어 추가 실패 - 50개 초과 → BadRequestException")
    void createWord_limitExceeded() {
        WordCreateRequestDto request = new WordCreateRequestDto("apple", "사과", null, null, null);
        given(wordRepository.count()).willReturn(50L);

        assertThatThrownBy(() -> wordService.createWord(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("단어 추가 실패 - 중복 영단어 → DuplicateException")
    void createWord_duplicateEnglish() {
        WordCreateRequestDto request = new WordCreateRequestDto("apple", "사과", null, null, null);
        given(wordRepository.count()).willReturn(0L);
        given(wordRepository.existsByEnglishAndDeletedFalse(anyString())).willReturn(true);

        assertThatThrownBy(() -> wordService.createWord(request))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("단어 수정 성공")
    void updateWord_success() {
        WordUpdateRequestDto request = new WordUpdateRequestDto(null, "사과(과일)", null, null, null);
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word));

        WordResponseDto result = wordService.updateWord(1L, request);

        assertThat(result.getKorean()).isEqualTo("사과(과일)");
    }

    @Test
    @DisplayName("단어 수정 실패 - 없는 단어 → NotFoundException")
    void updateWord_notFound() {
        WordUpdateRequestDto request = new WordUpdateRequestDto("banana", null, null, null, null);
        given(wordRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.updateWord(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("단어 수정 실패 - 다른 단어와 영단어 중복 → DuplicateException")
    void updateWord_duplicateEnglish() {
        WordUpdateRequestDto request = new WordUpdateRequestDto("banana", null, null, null, null);
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word));
        given(wordRepository.existsByEnglishAndIdNotAndDeletedFalse(anyString(), anyLong())).willReturn(true);

        assertThatThrownBy(() -> wordService.updateWord(1L, request))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("단어 삭제 성공")
    void deleteWord_success() {
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word));

        wordService.deleteWord(1L);

        verify(wordRepository).delete(word);
    }

    @Test
    @DisplayName("단어 삭제 실패 - 없는 단어 → NotFoundException")
    void deleteWord_notFound() {
        given(wordRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.deleteWord(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("단어 목록 조회 성공 - 페이지네이션")
    void getWordList_success() {
        Page<Word> page = new PageImpl<>(List.of(word));
        given(wordRepository.findAll(any(Pageable.class))).willReturn(page);

        WordListResponseDto result = wordService.getWordList(0, 20, "id,asc", 1L);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("단어 상세 조회 성공")
    void getWordDetail_success() {
        given(wordRepository.findById(anyLong())).willReturn(Optional.of(word));

        WordDetailResponseDto result = wordService.getWordDetail(1L, 1L);

        assertThat(result.getEnglish()).isEqualTo("apple");
    }

    @Test
    @DisplayName("단어 상세 조회 실패 - 없는 단어 → NotFoundException")
    void getWordDetail_notFound() {
        given(wordRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.getWordDetail(999L, 1L))
                .isInstanceOf(NotFoundException.class);
    }
}
