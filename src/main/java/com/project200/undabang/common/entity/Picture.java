package com.project200.undabang.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "pictures")
public class Picture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "picture_id", nullable = false, updatable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "picture_name")
    private String pictureName;

    @Size(max = 10)
    @Column(name = "picture_extension", length = 10)
    private String pictureExtension;

    @Comment("바이트 단위")
    @Column(name = "picture_size")
    private Integer pictureSize;

    @Size(max = 255)
    @Column(name = "picture_url")
    private String pictureUrl;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "picture_created_at", nullable = false, updatable = false)
    private LocalDateTime pictureCreatedAt = LocalDateTime.now();

    @Column(name = "picture_deleted_at")
    private LocalDateTime pictureDeletedAt;

}