package com.project200.undabang.test;

import com.project200.undabang.common.validation.AllowedExtensions;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.notification.fcm.service.NotificationBatchService;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class TestController {

    private final NotificationBatchService notificationBatchService;
    private final NotificationService notificationService;
    private final TestService testService;

    private final JobLauncher jobLauncher;
    @Qualifier("decreaseExerciseScoreJob")
    private final Job decreaseExerciseScoreJob;

    @Qualifier("deleteExpiredFcmTokenJob")
    private final Job deleteExpiredFcmTokenJob;

    @PostMapping(value = "/test1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<String> handleFormSubmit2(@ModelAttribute TestDto1 testDto1) {
        return CommonResponse.success("success");
    }

    @PostMapping(
            value = "/test2",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public CommonResponse<String> handleFormSubmit1(@ModelAttribute TestDto1 testDto1) {
        return CommonResponse.success("success");
    }

    @PostMapping(value = "/test3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<String> handleFormSubmit3(@RequestPart TestDto2 testDto,
                                                    @RequestPart List<MultipartFile> files) {
        return CommonResponse.success("success");
    }

    @GetMapping("/test4")
    public ResponseEntity<Map<String, String>> testEndpoint(@RequestHeader HttpHeaders headers) {
        Map<String, String> headersMap = new HashMap<>();

        headers.forEach((key, value) -> headersMap.put(key, value.getFirst()));

        return ResponseEntity.ok(headersMap);
    }

    @PostMapping("/sendFcm")
    public void sendFcm() {
        notificationBatchService.sendInactivityNotifications();
    }

    // 운동점수 감소 수동 실행 엔드 포인트
    @PostMapping("/run-batch")
    public void runBatch() throws Exception {
        String runDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", runDate)
                .toJobParameters();

        jobLauncher.run(decreaseExerciseScoreJob, jobParameters);
    }

    @PostMapping("/delete-fcm-batch")
    public void deleteFcmBatch() throws Exception {
        String runDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", runDate) // 실행 식별자 역할 (중복 방지)
                .toJobParameters();

        jobLauncher.run(deleteExpiredFcmTokenJob, jobParameters);
    }

    /**
     * 테스트용 사진 여러 개를 업로드합니다.
     *
     * @param files 업로드할 파일 리스트
     * @return 생성된 Picture의 ID 목록 (List<Long>)
     */
    @PostMapping(path = "/api/v1/pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Long>> uploadPicturesForTest(@AllowedExtensions(extensions = {".jpg", ".jpeg", ".png"})
                                                            @RequestPart("pictures") List<MultipartFile> files) {
        List<Long> pictureIds = testService.uploadPictures(files);
        return ResponseEntity.ok(pictureIds);
    }

    /**
     * Picture ID 리스트를 받아 해당 사진들을 삭제 처리합니다.
     *
     * @param pictureIds 삭제할 Picture의 ID 목록
     * @return 처리 결과 메시지
     */
    @DeleteMapping("/api/v1/pictures")
    public ResponseEntity<CommonResponse<Void>> deletePicturesForTest(@RequestParam("pictureIds") List<Long> pictureIds) {
        testService.deletePictures(pictureIds);
        return ResponseEntity.ok(CommonResponse.success());
    }


}
