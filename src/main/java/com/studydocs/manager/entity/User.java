package com.studydocs.manager.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    @Column(unique = true,nullable = false, length = 100)
    private String username;
    @Column(unique = true,nullable = false, length = 150)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(length = 100)
    private String fullname;
    @Column(length = 20)
    private String phone;
    private Boolean enabled = true;
    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updateAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private  Set<Role> roles = new HashSet<>();

    public User(){
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @PrePersist
    protected void onUpdated() {
        createdAt = LocalDateTime.now();
        updateAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate(){
        updateAt = LocalDateTime.now();
    }
    public void addRole(Role role){
        this.roles.add(role);
        role.getUsers().add(this);
    }
    public void removeRole(Role role ){
        this.roles.remove(role);
        role.getUsers().remove(this);
    }
}
