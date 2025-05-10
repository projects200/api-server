package com.project200.undabang.post.entity;

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
@Table(name = "post_type")
public class PostType {
    @Id
    @Column(name = "post_type_id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "post_type_name", nullable = false)
    private String postTypeName;

    @Size(max = 255)
    @NotNull
    @Column(name = "post_type_desc", nullable = false)
    private String postTypeDesc;

}