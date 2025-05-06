package com.project200.undabang.report.entity;

import com.project200.undabang.report.enums.ReportProcessingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @Column(name = "report_id", nullable = false)
    private Long id;

    @Size(max = 500)
    @Column(name = "report_content", length = 500)
    private String reportContent;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "report_datetime", nullable = false)
    private LocalDateTime reportDatetime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "report_processing_status", nullable = false, length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'PENDING'")
    private ReportProcessingStatus reportProcessingStatus = ReportProcessingStatus.PENDING;

    @Column(name = "report_processed_at")
    private LocalDateTime reportProcessedAt;

    @Size(max = 500)
    @Column(name = "report_processing_content", length = 500)
    private String reportProcessingContent;

}