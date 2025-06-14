package com.project200.undabang.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Setter
    @Size(max = 255)
    @Column(name = "picture_url")
    private String pictureUrl;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "picture_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime pictureCreatedAt = LocalDateTime.now();

    @Column(name = "picture_deleted_at")
    private LocalDateTime pictureDeletedAt;

    public static Picture of(MultipartFile file, String pictureUrl) {
        // 파일 이름이 255자를 초과하는 경우, 255자로 잘라냅니다.
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        if (originalFilename.length() > 255) {
            originalFilename = originalFilename.substring(0, 255);
        }

        return Picture.builder()
                .pictureName(originalFilename)
                .pictureExtension(getFileExtension(file.getOriginalFilename()))
                .pictureSize((int) file.getSize())
                .pictureUrl(pictureUrl)
                .build();
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public Picture softDelete(){
        this.pictureDeletedAt = LocalDateTime.now();
        return this;
    }

}