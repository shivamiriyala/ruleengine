package com.example.ruleengine.entity;


//import javax.persistence.*;
import jakarta.persistence.*;

import javax.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "dependency_upload")
public class DependencyUpload {

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ciUploadId;

    private String email;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCiUploadId() { return ciUploadId; }
    public void setCiUploadId(String ciUploadId) { this.ciUploadId = ciUploadId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
