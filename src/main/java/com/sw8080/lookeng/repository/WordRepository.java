package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordRepository extends JpaRepository<Word, Long> {
    // 1. 단어 개수 제한(50개)을 위한 조회 (삭제되지 않은 것만)
    long countByDeletedFalse();

    // 2. 단어장 목록 조회용 (삭제되지 않은 것만 페이징해서 가져오기)
    Page<Word> findAllByDeletedFalse(Pageable pageable);

    // 3. 409 에러(단어 추가 시 중복) 처리: 삭제 안 된 단어 중 동일한 영단어가 있는지 확인
    boolean existsByEnglishAndDeletedFalse(String english);

    // 4. 409 에러(단어 수정 시 중복) 처리: 본인(id) 제외, 삭제 안 된 단어 중 동일한 영단어가 있는지 확인
    boolean existsByEnglishAndIdNotAndDeletedFalse(String english, Long id);


}
