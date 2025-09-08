package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.CreateProfilePictureResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MemberPictureCommandService {
    CreateProfilePictureResponse createProfilePicture(MultipartFile profilePicture);
}
