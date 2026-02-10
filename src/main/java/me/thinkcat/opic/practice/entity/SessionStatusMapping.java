package me.thinkcat.opic.practice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;

import java.sql.Types;

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
    @Column(name = "status_code", columnDefinition = "char(7)")
    @JdbcTypeCode(Types.CHAR)
    private String statusCode;

    @Column(name = "status_text", nullable = false)
    private String statusText;

    @Column(name = "description", nullable = false)
    private String description;
}
