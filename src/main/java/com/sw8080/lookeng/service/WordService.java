package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.WordItemDto;
import com.sw8080.lookeng.exception.BadRequestException;
import com.sw8080.lookeng.exception.DuplicateException;
import com.sw8080.lookeng.exception.NotFoundException;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;

    @Transactional
    public WordResponseDto createWord(WordCreateRequestDto request) {
        // 1. 단어 개수 50개 제한 로직 (@SQLRestriction으로 삭제된 단어 자동 제외)
        if (wordRepository.count() >= 50) {
            throw new BadRequestException("단어장에는 최대 50개의 단어만 추가할 수 있습니다.");
        }

        // 2. 삭제된 데이터 포함해서 영단어 존재 여부 확인
        Optional<Word> existingWord = wordRepository.findByEnglishIncludingDeleted(request.getEnglish());

        if (existingWord.isPresent()) {
            Word word = existingWord.get();

            // 이미 사용 중인 단어라면 중복 에러 발생
            if (!word.isDeleted()) {
                throw new DuplicateException("이미 존재하는 영단어입니다.");
            }

            // 삭제된 단어라면 상태를 변경하고 새로운 정보로 업데이트 (Restore)
            word.restore(request);
            return WordResponseDto.from(word);
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
        // 1. 명세서 404 에러: 수정할 단어가 존재하는지 조회 (@SQLRestriction으로 삭제된 단어 자동 제외)
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. 명세서 409 에러: 영단어(english) 수정 요청이 들어왔고, 그 값이 기존과 다를 경우 중복 검사
        if (request.getEnglish() != null && !request.getEnglish().equals(word.getEnglish())) {
            if (wordRepository.existsByEnglishAndIdNot(request.getEnglish(), id)) {
                throw new DuplicateException("이미 존재하는 영단어입니다.");
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
        // 1. 명세서 404 에러: 삭제할 단어가 존재하는지 조회 (@SQLRestriction으로 삭제된 단어 자동 제외)
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. 소프트 삭제 — @SQLDelete가 DELETE를 UPDATE word SET is_deleted=true 로 변환
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

        // 2. Pageable 객체 생성 및 DB 조회 (@SQLRestriction으로 삭제된 단어 자동 제외)
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
        // 1. 명세서 404 에러: 조회할 단어가 존재하는지 확인 (@SQLRestriction으로 삭제된 단어 자동 제외)
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. 나중에 USER_WORD 테이블이 생기면 연동할 부분 (지금은 임시로 false)
        boolean isMemorized = false;
        boolean isBookmarked = false;

        // 3. DTO로 변환하여 반환
        return WordDetailResponseDto.from(word, isMemorized, isBookmarked);
    }
}