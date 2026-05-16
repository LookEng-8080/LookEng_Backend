package com.sw8080.lookeng.repository;

import com.sw8080.lookeng.entity.UserWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWordRepository extends JpaRepository<UserWord, Long> {
    Optional<UserWord> findByUserIdAndWordId(Long userId, Long wordId);
    List<UserWord> findByUserIdAndIsBookmarkedTrue(Long userId);
    List<UserWord> findByUserIdAndIsMemorizedTrue(Long userId);
    List<UserWord> findByUserIdAndWordIdIn(Long userId, List<Long> wordIds);
}
