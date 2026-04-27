package com.sw8080.lookeng.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // JPA Entity 클래스들이 이 클래스를 상속할 경우 컬럼으로 인식하게 함
@EntityListeners(AuditingEntityListener.class) // 생성/수정 시간을 자동으로 기록해주는 스위치!
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false) // 한 번 생성되면 그 이후에는 수정되지 않도록 막음
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
