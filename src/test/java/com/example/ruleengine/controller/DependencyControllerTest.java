package com.example.ruleengine.controller;

import com.example.ruleengine.service.DebrickedService;
import com.example.ruleengine.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DependencyControllerTest {

    @Mock
    private DebrickedService debrickedService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DependencyController dependencyController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks before each test
    }

    @Test
    void uploadFiles_SuccessfulUpload_ReturnsOkResponse() throws Exception {
        // Arrange
        MultipartFile[] files = new MultipartFile[1]; // Mock file array
        String email = "test@example.com";

        // Mock the behavior of the service method
        when(debrickedService.uploadDependencies(files, email)).thenReturn(true);

        // Act
        ResponseEntity<String> response = dependencyController.uploadFiles(files, email);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Files uploaded successfully", response.getBody());

        // Verify that the service method was called
        verify(debrickedService, times(1)).uploadDependencies(files, email);
        verifyNoInteractions(notificationService); // No email notification for success
    }

    @Test
    void uploadFiles_FailedUpload_ReturnsBadRequestResponse() throws Exception {
        // Arrange
        MultipartFile[] files = new MultipartFile[1];
        String email = "test@example.com";

        // Mock the behavior of the service method to return false
        when(debrickedService.uploadDependencies(files, email)).thenReturn(false);

        // Act
        ResponseEntity<String> response = dependencyController.uploadFiles(files, email);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File Upload Failed", response.getBody());

        // Verify the service method was called and no email notification was sent
        verify(debrickedService, times(1)).uploadDependencies(files, email);
        verifyNoInteractions(notificationService); // No email notification for failed upload
    }

    @Test
    void uploadFiles_ExceptionDuringUpload_ReturnsInternalServerError() throws Exception {
        // Arrange
        MultipartFile[] files = new MultipartFile[1];
        String email = "test@example.com";
        String errorMessage = "An error occurred";

        // Mock the behavior of the service method to throw an exception
        when(debrickedService.uploadDependencies(files, email)).thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<String> response = dependencyController.uploadFiles(files, email);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("File upload failed: " + errorMessage, response.getBody());

        // Verify that the service method was called and email notification was sent
        verify(debrickedService, times(1)).uploadDependencies(files, email);
        verify(notificationService, times(1)).sendEmail(email, "File upload failed", errorMessage);
    }
}
