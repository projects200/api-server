package com.project200.undabang.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * AllowedSingleMultipartFileExtensionsValidator는 @AllowedExtensions 어노테이션을 처리하는 클래스입니다.
 * 특정 MultipartFile 객체의 파일 확장자가 허용된 확장자 목록에 포함되는지 검증합니다.
 * <p>
 * 이 클래스는 ConstraintValidator<AllowedExtensions, MultipartFile>를 구현하며,
 * Spring의 MultipartFile을 사용하여 파일 이름과 확장자를 확인합니다.
 */
public class AllowedSingleMultipartFileExtensionsValidator implements ConstraintValidator<AllowedExtensions, MultipartFile> {

    private String[] extensions;

    @Override
    public void initialize(AllowedExtensions constraintAnnotation) {
        this.extensions = constraintAnnotation.extensions();
    }

    /**
     * 주어진 MultipartFile이 유효한 파일 확장자를 가지고 있는지 검증합니다.
     * 파일이 null이거나 비어있을 경우 유효한 것으로 간주합니다.
     *
     * @param multipartFile 검사할 MultipartFile 객체
     * @param context       검증 시 사용되는 ConstraintValidatorContext 객체
     * @return 파일이 유효한 확장자를 가지고 있으면 true, 그렇지 않으면 false
     */
    @Override
    public boolean isValid(MultipartFile multipartFile, ConstraintValidatorContext context) {
        // 파일이 없으면 검증 통과 (@NotNull이 담당할 영역)
        if (multipartFile == null || multipartFile.isEmpty()) {
            return true;
        }

        String filename = multipartFile.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        // 기존의 확장자 검증 로직을 그대로 사용
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
