package me.thinkcat.opic.practice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags")
@Getter
@NoArgsConstructor
public class FeatureFlag {

    @Id
    @Column(name = "flag_name", length = 100)
    private String flagName;

    @Column(nullable = false)
    private boolean enabled;

    @Column
    private String description;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
