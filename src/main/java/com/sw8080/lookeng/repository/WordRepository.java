package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordRepository extends JpaRepository<Word, Long> {
    // 409 에러(중복) 처리를 위해 영단어 존재 여부 확인 메서드 추가
    boolean existsByEnglish(String english);

    // 409 에러 처리: 본인(id)을 제외하고 동일한 영단어가 존재하는지 확인
    boolean existsByEnglishAndIdNot(String english, Long id);

}
