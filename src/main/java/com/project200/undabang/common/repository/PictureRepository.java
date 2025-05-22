package com.project200.undabang.common.repository;

import com.project200.undabang.common.entity.Picture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PictureRepository extends JpaRepository<Picture, Long> {
}
