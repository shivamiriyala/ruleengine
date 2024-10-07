package com.example.ruleengine.service;

import com.example.ruleengine.entity.DependencyUpload;
import com.example.ruleengine.repository.DependencyUploadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScanStatusCheckerServiceTest {

    @InjectMocks
    private ScanStatusCheckerService scanStatusCheckerService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DependencyUploadRepository repository;

    @Mock
    private NotificationService notificationService;

    private String apiUsername = "testUsername";
    private String apiPassword = "testPassword";
    private String debrickedApiUrl = "https://debricked.com/api/";
    private int vulnerabilitiesThreshold = 5; // Set your threshold

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set the private fields
        ReflectionTestUtils.setField(scanStatusCheckerService, "debrickedApiUrl", debrickedApiUrl);
        ReflectionTestUtils.setField(scanStatusCheckerService, "vulnerabilitiesThreshold", vulnerabilitiesThreshold);
        ReflectionTestUtils.setField(scanStatusCheckerService, "apiUsername", apiUsername);
        ReflectionTestUtils.setField(scanStatusCheckerService, "apiPassword", apiPassword);
    }

    @Test
    void testCheckScanStatus_SuccessfulUpload() throws Exception {
        // Mock the behavior of the repository
        DependencyUpload upload = new DependencyUpload();
        upload.setCiUploadId("12345");
        upload.setEmail("test@example.com");
        upload.setStatus(DependencyUpload.Status.IN_PROGRESS);

        when(repository.findAllByStatus(DependencyUpload.Status.IN_PROGRESS))
                .thenReturn(Arrays.asList(upload));

        // Mock the JWT token retrieval
        String mockToken = "mocked-jwt-token"; // Create a mock token
        ReflectionTestUtils.setField(scanStatusCheckerService, "apiUsername", apiUsername);
        ReflectionTestUtils.setField(scanStatusCheckerService, "apiPassword", apiPassword);
        // Instead of calling getJwtToken(), directly set the expected behavior for a successful call
        when(scanStatusCheckerService.getJwtToken(apiUsername, apiPassword)).thenReturn(mockToken);

        // Mock the behavior of the RestTemplate
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("progress", 100);
        responseBody.put("vulnerabilitiesFound", 10);

        ResponseEntity responseEntity = mock(ResponseEntity.class);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn(responseBody);

        when(restTemplate.exchange(
                any(String.class),
                any(),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(responseEntity);

        // Call the method under test
        scanStatusCheckerService.checkScanStatus();

        // Verify the repository and notification service interactions
        verify(repository, times(1)).save(upload);
        verify(notificationService, times(1)).sendEmail(
                eq("test@example.com"),
                any(String.class),
                any(String.class)
        );

        // Verify that the upload status was changed
        assertEquals(DependencyUpload.Status.COMPLETED, upload.getStatus());
    }

    // Add more test cases as needed
}
