package com.project200.undabang.report.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "post_report_subjects")
public class PostReportSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_report_subject_id", nullable = false, updatable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "post_report_subject_name", nullable = false)
    private String postReportSubjectName;

}