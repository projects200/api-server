package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.CreateProfilePictureResponse;
import com.project200.undabang.member.dto.response.UpdateProfilePictureResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MemberPictureCommandService {
    CreateProfilePictureResponse createProfilePicture(MultipartFile profilePicture);
    UpdateProfilePictureResponse updateRepresentativeProfileImage(Long pictureId);
    void deleteProfilePicture(Long pictureId);
}
