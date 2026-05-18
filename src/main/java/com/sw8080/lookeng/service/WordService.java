package com.sw8080.lookeng.service;

import com.sw8080.lookeng.dto.WordItemDto;
import com.sw8080.lookeng.dto.response.BulkUploadResponseDto;
import com.sw8080.lookeng.dto.response.WordSearchResponseDto;
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

    private static final String CSV_HEADER = "english,korean,partOfSpeech,exampleSentence,pronunciationUrl";
    private static final int MAX_BULK_COUNT = 1000;

    @Transactional
    public WordResponseDto createWord(WordCreateRequestDto request) {
        // 1. 단어 개수 50개 제한
        if (wordRepository.count() >= 50) {
            throw new BadRequestException("단어장에는 최대 50개의 단어만 추가할 수 있습니다.");
        }

        // 2. 삭제된 데이터 포함해서 영단어 존재 여부 확인
        Optional<Word> existingWord = wordRepository.findByEnglishIncludingDeleted(request.getEnglish());

        if (existingWord.isPresent()) {
            Word word = existingWord.get();
            if (!word.isDeleted()) {
                throw new DuplicateException("이미 존재하는 영단어입니다.");
            }
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
        // 1. 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("CSV 파일만 업로드 가능합니다.");
        }

        // 2. CSV 파싱 및 행별 유효성 검사
        List<WordCreateRequestDto> validWords = new ArrayList<>();
        Set<String> seenEnglish = new HashSet<>();

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

                if (validWords.size() >= MAX_BULK_COUNT) {
                    throw new BadRequestException("한 번에 최대 " + MAX_BULK_COUNT + "개의 단어만 업로드 가능합니다.");
                }

                String[] cols = parseCsvLine(line);
                String english = cols.length > 0 ? cols[0].trim() : "";
                String korean  = cols.length > 1 ? cols[1].trim() : "";

                if (english.isEmpty()) {
                    throw new BadRequestException(rowNum + "행: 영단어(english)는 필수입니다.");
                }
                if (korean.isEmpty()) {
                    throw new BadRequestException(rowNum + "행: 뜻(korean)은 필수입니다.");
                }

                if (!seenEnglish.add(english.toLowerCase())) {
                    throw new DuplicateException(rowNum + "행: CSV 내 중복 영단어 '" + english + "'가 있습니다.");
                }

                String partOfSpeech     = cols.length > 2 ? emptyToNull(cols[2]) : null;
                String exampleSentence  = cols.length > 3 ? emptyToNull(cols[3]) : null;
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

        // 5. 삭제된 단어 포함 일괄 조회
        List<String> englishList = validWords.stream()
                .map(WordCreateRequestDto::getEnglish)
                .collect(Collectors.toList());
        List<Word> existingWords = wordRepository.findAllByEnglishIncludingDeleted(englishList);

        // 5-1. 활성화된 단어(중복) vs 소프트딜리트된 단어(복구 가능) 분리
        Map<String, Word> existingMap = existingWords.stream()
                .collect(Collectors.toMap(Word::getEnglish, w -> w));

        List<String> activeDuplicates = existingWords.stream()
                .filter(w -> !w.isDeleted())
                .map(Word::getEnglish)
                .collect(Collectors.toList());

        if (!activeDuplicates.isEmpty()) {
            throw new DuplicateException("이미 등록된 영단어가 포함되어 있습니다: " + String.join(", ", activeDuplicates));
        }

        // 6. 단어 개수 50개 제한 확인 (소프트딜리트 복구는 카운트 제외)
        long activeCount = wordRepository.count();
        long newWordCount = validWords.stream()
                .filter(dto -> !existingMap.containsKey(dto.getEnglish()))
                .count();
        if (activeCount + newWordCount > 50) {
            throw new BadRequestException(
                    "단어장 최대 개수(50개)를 초과합니다. 현재 " + activeCount + "개, 신규 추가 " + newWordCount + "개");
        }

        // 7. 소프트딜리트된 단어 복구 + 새 단어 저장
        int count = 0;
        for (WordCreateRequestDto dto : validWords) {
            Word existing = existingMap.get(dto.getEnglish());
            if (existing != null && existing.isDeleted()) {
                // 소프트딜리트된 단어 복구
                existing.restore(dto);
                count++;
            } else {
                // 새 단어 생성
                wordRepository.save(Word.builder()
                        .english(dto.getEnglish())
                        .korean(dto.getKorean())
                        .partOfSpeech(dto.getPartOfSpeech())
                        .exampleSentence(dto.getExampleSentence())
                        .pronunciationUrl(dto.getPronunciationUrl())
                        .build());
                count++;
            }
        }

        return BulkUploadResponseDto.builder()
                .totalRequested(validWords.size())
                .successCount(count)
                .failCount(0)
                .build();
    }

    @Transactional(readOnly = true)
    public WordSearchResponseDto searchWords(String keyword, int page, int size, Long userId) {
        // 1. 키워드 유효성 검사
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BadRequestException("검색어를 입력해주세요.");
        }

        // 2. 페이지네이션 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "english"));

        // 3. DB 검색 (영단어 또는 한글 뜻 부분 일치, 대소문자 무시)
        Page<Word> wordPage = wordRepository.searchByKeyword(keyword.trim(), pageable);

        // 4. USER_WORD 조회로 isMemorized / isBookmarked 반영
        List<Long> wordIds = wordPage.getContent().stream()
                .map(Word::getId)
                .collect(Collectors.toList());
        Map<Long, UserWord> userWordMap = userWordRepository.findByUserIdAndWordIdIn(userId, wordIds)
                .stream()
                .collect(Collectors.toMap(UserWord::getWordId, uw -> uw));

        // 5. 엔티티 → DTO 변환
        List<WordItemDto> content = wordPage.getContent().stream()
                .map(word -> {
                    UserWord uw = userWordMap.get(word.getId());
                    boolean isMemorized = uw != null && uw.isMemorized();
                    boolean isBookmarked = uw != null && uw.isBookmarked();
                    return WordItemDto.from(word, isMemorized, isBookmarked);
                })
                .collect(Collectors.toList());

        // 6. 응답 DTO 조립
        return WordSearchResponseDto.builder()
                .content(content)
                .pageInfo(WordSearchResponseDto.PageInfo.builder()
                        .page(wordPage.getNumber())
                        .size(wordPage.getSize())
                        .totalElements(wordPage.getTotalElements())
                        .totalPages(wordPage.getTotalPages())
                        .build())
                .build();
    }

    @Transactional
    public WordResponseDto updateWord(Long id, WordUpdateRequestDto request) {
        // 1. 수정할 단어 조회
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        // 2. 영단어 중복 검사
        if (request.getEnglish() != null && !request.getEnglish().equals(word.getEnglish())) {
            if (wordRepository.existsByEnglishAndIdNot(request.getEnglish(), id)) {
                throw new DuplicateException("이미 존재하는 영단어입니다.");
            }
        }

        // 3. 단어 정보 수정
        word.update(
                request.getEnglish(),
                request.getKorean(),
                request.getPartOfSpeech(),
                request.getExampleSentence(),
                request.getPronunciationUrl()
        );

        return WordResponseDto.from(word);
    }

    @Transactional
    public void deleteWord(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

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

        // 2. DB 조회
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Word> wordPage = wordRepository.findAll(pageable);

        // 3. USER_WORD 일괄 조회
        List<Long> wordIds = wordPage.getContent().stream()
                .map(Word::getId)
                .collect(Collectors.toList());
        Map<Long, UserWord> userWordMap = userWordRepository.findByUserIdAndWordIdIn(userId, wordIds)
                .stream()
                .collect(Collectors.toMap(UserWord::getWordId, uw -> uw));

        // 4. DTO 변환
        List<WordItemDto> content = wordPage.getContent().stream()
                .map(word -> {
                    UserWord uw = userWordMap.get(word.getId());
                    boolean isMemorized = uw != null && uw.isMemorized();
                    boolean isBookmarked = uw != null && uw.isBookmarked();
                    return WordItemDto.from(word, isMemorized, isBookmarked);
                })
                .collect(Collectors.toList());

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
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 단어를 찾을 수 없습니다."));

        Optional<UserWord> userWordOpt = userWordRepository.findByUserIdAndWordId(userId, id);
        boolean isMemorized = userWordOpt.map(UserWord::isMemorized).orElse(false);
        boolean isBookmarked = userWordOpt.map(UserWord::isBookmarked).orElse(false);

        return WordDetailResponseDto.from(word, isMemorized, isBookmarked);
    }

    // ── CSV 파싱 헬퍼 ──────────────────────────────────────────────────────────

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

    private String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
