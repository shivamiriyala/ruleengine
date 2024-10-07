package com.example.ruleengine.repository;

import com.example.ruleengine.entity.DependencyUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DependencyUploadRepository extends JpaRepository<DependencyUpload, Long> {
    List<DependencyUpload> findAllByStatus(DependencyUpload.Status status);
}
