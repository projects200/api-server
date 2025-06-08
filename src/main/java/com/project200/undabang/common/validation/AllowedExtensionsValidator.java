package com.project200.undabang.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AllowedExtensionsValidator는 @AllowedExtensions 어노테이션을 처리하는 클래스입니다.
 * 주어진 MultipartFile 리스트 내 모든 파일의 확장자가 지정된 허용된 확장자 목록에 포함되는지 검증하는 역할을 합니다.
 *
 * 이 클래스는 ConstraintValidator<AllowedExtensions, List<MultipartFile>>를 구현하며,
 * Spring의 MultipartFile을 사용하여 파일 이름과 확장자를 검증합니다.
 */
public class AllowedExtensionsValidator implements ConstraintValidator<AllowedExtensions, List<MultipartFile>> {
    private String[] extensions;

    @Override
    public void initialize(AllowedExtensions constraintAnnotation) {
        this.extensions = constraintAnnotation.extensions();
    }

    /**
     * 주어진 MultipartFile 리스트가 유효한 파일 확장자를 가지고 있는지 검증합니다.
     * 파일 리스트가 null일 경우 유효한 것으로 간주합니다.
     *
     * @param multipartFileList 검사할 MultipartFile 리스트
     * @param constraintValidatorContext 검증 시 사용되는 ConstraintValidatorContext 객체
     * @return 모든 파일이 유효한 확장자를 가지고 있으면 true, 그렇지 않으면 false
     */
    @Override
    public boolean isValid(List<MultipartFile> multipartFileList, ConstraintValidatorContext constraintValidatorContext) {
        if (multipartFileList == null) return true;
        for (MultipartFile multipartFile : multipartFileList) {
            if (!isFileValid(multipartFile)) return false;
        }
        return true;
    }

    private boolean isFileValid(MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        if (filename == null) return false;
        return hasValidExtension(filename);
    }

    private boolean hasValidExtension(String filename) {
        for (String ext : extensions) {
            if (filename.toLowerCase().endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
