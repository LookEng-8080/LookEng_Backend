package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.WordItemDto;
import com.sw8080.lookeng.dto.response.BulkUploadResponseDto;
import com.sw8080.lookeng.exception.BadRequestException;
import com.sw8080.lookeng.exception.DuplicateException;
import com.sw8080.lookeng.exception.NotFoundException;
import com.sw8080.lookeng.dto.request.WordCreateRequestDto;
import com.sw8080.lookeng.dto.response.WordDetailResponseDto;
import com.sw8080.lookeng.dto.response.WordListResponseDto;
import com.sw8080.lookeng.dto.response.WordResponseDto;
import com.sw8080.lookeng.dto.response.WordUpdateRequestDto;
import com.sw8080.lookeng.entity.UserWord;
import com.sw8080.lookeng.entity.Word;
import com.sw8080.lookeng.repository.UserWordRepository;
import com.sw8080.lookeng.repository.WordRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;

    // CSV 헤더 포맷 상수
    private static final String CSV_HEADER = "english,korean,partOfSpeech,exampleSentence,pronunciationUrl";
    private static final int MAX_BULK_COUNT = 1000;

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
    public BulkUploadResponseDto bulkUpload(MultipartFile file) {
        // 1. 확장자 검증 (.csv만 허용)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("CSV 파일만 업로드 가능합니다.");
        }

        // 2. CSV 파싱 및 행별 유효성 검사
        List<WordCreateRequestDto> validWords = new ArrayList<>();
        Set<String> seenEnglish = new HashSet<>(); // CSV 내부 중복 검사용

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // 3. 헤더 검증
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BadRequestException("파일이 비어 있습니다.");
            }
            if (!headerLine.trim().equals(CSV_HEADER)) {
                throw new BadRequestException("CSV 헤더가 올바르지 않습니다. 예상: " + CSV_HEADER);
            }

            // 4. 행별 파싱
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;

                // 1000행 초과 검사
                if (validWords.size() >= MAX_BULK_COUNT) {
                    throw new BadRequestException("한 번에 최대 " + MAX_BULK_COUNT + "개의 단어만 업로드 가능합니다.");
                }

                String[] cols = parseCsvLine(line);
                String english = cols.length > 0 ? cols[0].trim() : "";
                String korean  = cols.length > 1 ? cols[1].trim() : "";

                // 필수값 검증
                if (english.isEmpty()) {
                    throw new BadRequestException(rowNum + "행: 영단어(english)는 필수입니다.");
                }
                if (korean.isEmpty()) {
                    throw new BadRequestException(rowNum + "행: 뜻(korean)은 필수입니다.");
                }

                // CSV 내 중복 검사
                if (!seenEnglish.add(english.toLowerCase())) {
                    throw new DuplicateException(rowNum + "행: CSV 내 중복 영단어 '" + english + "'가 있습니다.");
                }

                String partOfSpeech    = cols.length > 2 ? emptyToNull(cols[2]) : null;
                String exampleSentence = cols.length > 3 ? emptyToNull(cols[3]) : null;
                String pronunciationUrl = cols.length > 4 ? emptyToNull(cols[4]) : null;

                validWords.add(new WordCreateRequestDto(
                        english, korean, partOfSpeech, exampleSentence, pronunciationUrl));
            }

        } catch (IOException e) {
            throw new BadRequestException("파일을 읽는 중 오류가 발생했습니다.");
        }

        if (validWords.isEmpty()) {
            throw new BadRequestException("업로드할 단어가 없습니다.");
        }

        // 5. DB 중복 일괄 검사 (루프 대신 쿼리 1번으로 처리)
        List<String> englishList = validWords.stream()
                .map(WordCreateRequestDto::getEnglish)
                .collect(Collectors.toList());
        List<String> duplicates = wordRepository.findExistingEnglish(englishList);
        if (!duplicates.isEmpty()) {
            throw new DuplicateException("이미 등록된 영단어가 포함되어 있습니다: " + String.join(", ", duplicates));
        }

        // 6. 단어 개수 50개 제한 확인
        long currentCount = wordRepository.count();
        if (currentCount + validWords.size() > 50) {
            throw new BadRequestException(
                    "단어장 최대 개수(50개)를 초과합니다. 현재 " + currentCount + "개, 추가 요청 " + validWords.size() + "개");
        }

        // 7. 일괄 저장
        List<Word> words = validWords.stream()
                .map(dto -> Word.builder()
                        .english(dto.getEnglish())
                        .korean(dto.getKorean())
                        .partOfSpeech(dto.getPartOfSpeech())
                        .exampleSentence(dto.getExampleSentence())
                        .pronunciationUrl(dto.getPronunciationUrl())
                        .build())
                .collect(Collectors.toList());

        wordRepository.saveAll(words);

        int count = validWords.size();
        return BulkUploadResponseDto.builder()
                .totalRequested(count)
                .successCount(count)
                .failCount(0)
                .build();
    }

    @Transactional
    public WordResponseDto updateWord(Long id, WordUpdateRequestDto request) {
        // 1. 명세서 404 에러: 수정할 단어가 존재하는지 조회
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. 명세서 409 에러: 영단어(english) 수정 요청 시 중복 검사
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
        // 1. 명세서 404 에러: 삭제할 단어가 존재하는지 조회
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. 소프트 삭제 — @SQLDelete가 DELETE를 UPDATE word SET is_deleted=true 로 변환
        wordRepository.delete(word);
    }

    @Transactional(readOnly = true)
    public WordListResponseDto getWordList(int page, int size, String sortParam, Long userId) {

        // 1. 정렬 조건 파싱
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        if ("english,asc".equals(sortParam)) {
            sort = Sort.by(Sort.Direction.ASC, "english");
        } else if ("english,desc".equals(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "english");
        }

        // 2. Pageable 객체 생성 및 DB 조회
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Word> wordPage = wordRepository.findAll(pageable);

        // 3. 해당 페이지 wordId 목록으로 USER_WORD 일괄 조회
        List<Long> wordIds = wordPage.getContent().stream()
                .map(Word::getId)
                .collect(Collectors.toList());
        Map<Long, UserWord> userWordMap = userWordRepository.findByUserIdAndWordIdIn(userId, wordIds)
                .stream()
                .collect(Collectors.toMap(UserWord::getWordId, uw -> uw));

        // 4. 엔티티(Word) -> DTO 변환
        List<WordItemDto> content = wordPage.getContent().stream()
                .map(word -> {
                    UserWord uw = userWordMap.get(word.getId());
                    boolean isMemorized = uw != null && uw.isMemorized();
                    boolean isBookmarked = uw != null && uw.isBookmarked();
                    return WordItemDto.from(word, isMemorized, isBookmarked);
                })
                .collect(Collectors.toList());

        // 5. 응답 DTO 조립
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
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. USER_WORD 조회로 실제 학습 상태 반영
        Optional<UserWord> userWordOpt = userWordRepository.findByUserIdAndWordId(userId, id);
        boolean isMemorized = userWordOpt.map(UserWord::isMemorized).orElse(false);
        boolean isBookmarked = userWordOpt.map(UserWord::isBookmarked).orElse(false);

        // 3. DTO로 변환하여 반환
        return WordDetailResponseDto.from(word, isMemorized, isBookmarked);
    }

    // ── CSV 파싱 헬퍼 ──────────────────────────────────────────────────────────

    // 따옴표로 감싼 필드(쉼표 포함 가능)를 처리하는 CSV 라인 파서
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    // 빈 문자열을 null로 변환 (선택 필드 처리용)
    private String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
