package com.project200.undabang.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "post_report_subjects")
public class PostReportSubject {
    @Id
    @Column(name = "post_report_subject_id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "post_report_subject_name", nullable = false)
    private String postReportSubjectName;

}