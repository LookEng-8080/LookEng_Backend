package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.WordItemDto;
import com.sw8080.lookeng.dto.request.WordCreateRequestDto;
import com.sw8080.lookeng.dto.response.WordDetailResponseDto;
import com.sw8080.lookeng.dto.response.WordListResponseDto;
import com.sw8080.lookeng.dto.response.WordResponseDto;
import com.sw8080.lookeng.dto.response.WordUpdateRequestDto;
import com.sw8080.lookeng.entity.Word;
import com.sw8080.lookeng.repository.WordRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;

    @Transactional
    public WordResponseDto createWord(WordCreateRequestDto request) {
        // 1. 단어 개수 50개 제한 로직 추가
        if (wordRepository.count() >= 50) {
            // 프로젝트 예외 처리 방식에 맞춰 400 Bad Request 또는 적절한 상태 코드로 응답하도록 변경하셔도 됩니다.
            throw new IllegalStateException("단어장에는 최대 50개의 단어만 추가할 수 있습니다.");
        }

        // 2. 명세서 409 에러: 동일 영단어 존재 여부 확인
        if (wordRepository.existsByEnglish(request.getEnglish())) {
            throw new IllegalStateException("이미 존재하는 영단어입니다.");
        }

        // 3. 단어 엔티티 생성 및 저장
        Word word = Word.builder()
                .english(request.getEnglish())
                .korean(request.getKorean())
                .partOfSpeech(request.getPartOfSpeech())
                .exampleSentence(request.getExampleSentence())
                .pronunciationUrl(request.getPronunciationUrl())
                .build();

        Word savedWord = wordRepository.save(word);

        // 4. 응답 DTO 변환
        return WordResponseDto.from(savedWord);
    }
    @Transactional
    public WordResponseDto updateWord(Long id, WordUpdateRequestDto request) {
        // 1. 명세서 404 에러: 수정할 단어가 존재하는지 조회
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 단어를 찾을 수 없습니다."));

        // 2. 명세서 409 에러: 영단어(english) 수정 요청이 들어왔고, 그 값이 기존과 다를 경우 중복 검사
        if (request.getEnglish() != null && !request.getEnglish().equals(word.getEnglish())) {
            if (wordRepository.existsByEnglishAndIdNot(request.getEnglish(), id)) {
                throw new IllegalStateException("이미 존재하는 영단어입니다."); // GlobalExceptionHandler에서 409 처리
            }
        }

        // 3. 단어 정보 수정 (Dirty Checking 발동)
        word.update(
                request.getEnglish(),
                request.getKorean(),
                request.getPartOfSpeech(),
                request.getExampleSentence(),
                request.getPronunciationUrl()
        );

        // 4. 응답 DTO 변환 후 반환
        return WordResponseDto.from(word);
    }

    @Transactional
    public void deleteWord(Long id) {
        // 1. 명세서 404 에러: 삭제할 단어가 존재하는지 조회
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 단어를 찾을 수 없습니다."));

        // 2. 단어 삭제
        wordRepository.delete(word);
    }

    @Transactional(readOnly = true)
    public WordListResponseDto getWordList(int page, int size, String sortParam, Long userId) {

        // 1. 명세서에 따른 정렬(Sort) 조건 파싱
        Sort sort = Sort.by(Sort.Direction.ASC, "id"); // 기본값: id,asc
        if ("english,asc".equals(sortParam)) {
            sort = Sort.by(Sort.Direction.ASC, "english");
        } else if ("english,desc".equals(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "english");
        }

        // 2. Pageable 객체 생성 및 DB 조회
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Word> wordPage = wordRepository.findAll(pageable);

        // 3. 엔티티(Word) -> DTO 변환
        List<WordItemDto> content = wordPage.getContent().stream()
                // 지금은 USER_WORD 테이블이 없으므로 임시로 false 꽂아넣기!
                .map(word -> WordItemDto.from(word, false, false))
                .collect(Collectors.toList());

        // 4. 명세서 포맷에 맞게 응답 DTO 조립
        return WordListResponseDto.builder()
                .content(content)
                .totalElements(wordPage.getTotalElements())
                .totalPages(wordPage.getTotalPages())
                .currentPage(wordPage.getNumber())
                .size(wordPage.getSize())
                .build();
    }
    @Transactional(readOnly = true)
    public WordDetailResponseDto getWordDetail(Long id, Long userId) {
        // 1. 명세서 404 에러: 조회할 단어가 존재하는지 확인
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 단어를 찾을 수 없습니다."));

        // 2. 나중에 USER_WORD 테이블이 생기면 연동할 부분 (지금은 임시로 false)
        boolean isMemorized = false;
        boolean isBookmarked = false;

        // 3. DTO로 변환하여 반환
        return WordDetailResponseDto.from(word, isMemorized, isBookmarked);
    }

}
