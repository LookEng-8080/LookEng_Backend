package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {
    // @SQLRestriction("is_deleted = false") 가 모든 쿼리에 자동 적용되므로
    // DeletedFalse 조건 없이도 삭제된 단어가 자동 제외됨

    // 409 에러(단어 추가 시 중복) 처리
    boolean existsByEnglish(String english);

    // 409 에러(단어 수정 시 중복) 처리: 본인(id) 제외
    boolean existsByEnglishAndIdNot(String english, Long id);

    // @SQLRestriction을 무시하고 삭제된 데이터까지 포함해서 조회
    @Query(value = "SELECT * FROM word w WHERE w.english = :english", nativeQuery = true)
    Optional<Word> findByEnglishIncludingDeleted(@Param("english") String english);

    // CSV 일괄 업로드 시 DB 중복 영단어 일괄 조회 (루프 대신 쿼리 1번으로 처리)
    @Query("SELECT w.english FROM Word w WHERE w.english IN :englishList")
    List<String> findExistingEnglish(@Param("englishList") List<String> englishList);
}
