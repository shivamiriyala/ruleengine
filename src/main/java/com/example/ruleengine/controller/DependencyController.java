package com.example.ruleengine.controller;

import com.example.ruleengine.service.DebrickedService;
import com.example.ruleengine.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dependencies") // Base URL for dependency-related API endpoints
public class DependencyController {

    @Autowired
    private DebrickedService debrickedService; // Service for handling dependency uploads

    @Autowired
    private NotificationService notificationService; // Service for sending notifications

    /**
     * Endpoint for uploading dependency files.
     *
     * @param files An array of files to be uploaded
     * @param email The email address to notify upon success or failure
     * @return ResponseEntity containing the status of the upload
     */
    @PostMapping("/upload") // Maps HTTP POST requests to this method
    public ResponseEntity<String> uploadFiles(@RequestParam("files") MultipartFile[] files, @RequestParam("email") String email) {
        try {
            // Call service to handle the file upload
            boolean status = debrickedService.uploadDependencies(files, email);
            if(status) {
                return ResponseEntity.ok("Files uploaded successfully"); // Return success response
            } else {
                return ResponseEntity.badRequest().body("File Upload Failed"); // Return failed response
            }

        } catch (Exception e) {
            // Log error message for debugging
            System.out.println("File upload failed: " + e.getMessage());

            // Send email notification about the failure
            notificationService.sendEmail(email, "File upload failed", e.getMessage());
            // Return error response with message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }
}
