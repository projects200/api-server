package com.project200.undabang.report.entity;

import com.project200.undabang.report.enums.ReportProcessingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false, updatable = false)
    private Long id;

    @Size(max = 500)
    @Column(name = "report_content", length = 500)
    private String reportContent;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "report_datetime", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime reportDatetime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "report_processing_status", length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private ReportProcessingStatus reportProcessingStatus = ReportProcessingStatus.PENDING;

    @Column(name = "report_processed_at")
    private LocalDateTime reportProcessedAt;

    @Size(max = 500)
    @Column(name = "report_processing_content", length = 500)
    private String reportProcessingContent;

}