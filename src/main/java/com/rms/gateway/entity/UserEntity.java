package com.rms.gateway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to admin-service {@code users} table (read-only from gateway).
 * Schema: id, image_id, name, email, password, jwt_secret, phone_no, role_id, address_id, is_verified, created_at, updated_at.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "jwt_secret", nullable = false, length = 512)
    private String jwtSecret;

    @Column(name = "phone_no", length = 50)
    private String phoneNo;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
