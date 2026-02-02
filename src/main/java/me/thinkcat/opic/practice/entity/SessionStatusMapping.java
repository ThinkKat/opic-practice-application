package me.thinkcat.opic.practice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "session_status_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class SessionStatusMapping extends BaseEntity {

    @Id
    @Column(length = 7)
    private String statusCode;

    @Column(nullable = false)
    private String statusText;

    @Column(nullable = false)
    private String description;
}
