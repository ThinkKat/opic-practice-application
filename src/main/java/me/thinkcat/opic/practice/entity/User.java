package me.thinkcat.opic.practice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;

import java.sql.Types;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private LocalDateTime termsAgreedAt;

    @Column
    private LocalDateTime privacyAgreedAt;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "user_role_code", nullable = false, columnDefinition = "char(7)")
    @Builder.Default
    private String userRoleCode = UserRole.FREE.getCode();

    public UserRole getUserRole() {
        return UserRole.fromCode(userRoleCode);
    }
}
