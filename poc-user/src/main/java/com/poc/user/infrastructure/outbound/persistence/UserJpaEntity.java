package com.poc.user.infrastructure.outbound.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", schema = "poc_user")
public class UserJpaEntity {

    @Id
    private String id;

    private String username;

    private String email;

    public UserJpaEntity() {}

    public UserJpaEntity(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
