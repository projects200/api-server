package com.project200.undabang.report.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "member_report_subjects")
public class MemberReportSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_report_subject_id", nullable = false, updatable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "member_report_subject_name", nullable = false)
    private String memberReportSubjectName;

}