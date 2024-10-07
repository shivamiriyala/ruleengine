package com.example.ruleengine.service;

import com.example.ruleengine.entity.DependencyUpload;
import com.example.ruleengine.repository.DependencyUploadRepository;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DebrickedServiceTest {

    @InjectMocks
    private DebrickedService debrickedService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DependencyUploadRepository repository;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private InputStream inputStream;

    @Value("${debricked.api.url}")
    private String debrickedApiUrl = "https://debricked.com/api/";

    @Value("${debricked.api.username}")
    private String apiUsername = "testUser";

    @Value("${debricked.api.password}")
    private String apiPassword = "testPass";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testUploadDependencies_SuccessfulUpload() throws IOException {
        // Arrange
        MultipartFile[] files = {multipartFile};

        // Mocking MultipartFile behavior
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));
        when(multipartFile.getOriginalFilename()).thenReturn("testfile");

        // Mocking CloseableHttpResponse and its associated objects
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        org.apache.http.StatusLine statusLine = mock(org.apache.http.StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        // Mocking HttpEntity
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity); // Mock the entity

        // Set up InputStream for the HttpEntity content
        InputStream inputStream = new ByteArrayInputStream("{\"ciUploadId\": \"uploadId\"}".getBytes());
        when(httpEntity.getContent()).thenReturn(inputStream); // Return InputStream

        // Ensure getContentLength returns a long value
        long contentLength = "{\"ciUploadId\": \"uploadId\"}".getBytes().length; // Calculate length as long
        when(httpEntity.getContentLength()).thenReturn(contentLength); // Correctly returns long

        // Mock EntityUtils to return a String from the HttpEntity
        when(EntityUtils.toString(httpEntity)).thenReturn("{\"ciUploadId\": \"uploadId\"}");

        // Mock the repository save method
        doNothing().when(repository).save(any(DependencyUpload.class));

        // Act
        boolean result = debrickedService.uploadDependencies(files, "test@example.com");

        // Assert
        assertTrue(result);
        verify(repository, times(1)).save(any(DependencyUpload.class));
    }


    @Test
    void testUploadDependencies_FailedUpload() throws IOException {
        MultipartFile[] files = {multipartFile};
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));
        when(multipartFile.getOriginalFilename()).thenReturn("testfile");

        // Mocking JWT token retrieval failure
        org.apache.http.StatusLine statusLine = mock(org.apache.http.StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(httpResponse.getStatusLine()).thenReturn(statusLine); // Return the mocked StatusLine

        boolean result = debrickedService.uploadDependencies(files, "test@example.com");

        assertFalse(result);
        verify(notificationService, times(1)).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void testGetJwtToken_Success() throws IOException {
        // Mocking JWT token retrieval
        org.apache.http.StatusLine statusLine = mock(org.apache.http.StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getStatusLine()).thenReturn(statusLine); // Return the mocked StatusLine

        // Mocking HttpEntity
        org.apache.http.HttpEntity httpEntity = mock(org.apache.http.HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity); // Mock the entity

        when(EntityUtils.toString(httpEntity)).thenReturn("{\"token\": \"testToken\"}");

        String token = debrickedService.getJwtToken(apiUsername, apiPassword);

        assertEquals("testToken", token);
    }

    @Test
    void testGetJwtToken_Failure() throws IOException {
        // Mocking JWT token retrieval failure
        org.apache.http.StatusLine statusLine = mock(org.apache.http.StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(httpResponse.getStatusLine()).thenReturn(statusLine); // Return the mocked StatusLine

        assertThrows(IOException.class, () -> debrickedService.getJwtToken(apiUsername, apiPassword));
    }

    @Test
    void testStartScan_Success() throws IOException {
        // Mocking scan start success
        org.apache.http.StatusLine statusLine = mock(org.apache.http.StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getStatusLine()).thenReturn(statusLine); // Return the mocked StatusLine

        assertDoesNotThrow(() -> debrickedService.startScan("uploadId", "testToken"));
    }

    @Test
    void testStartScan_Failure() throws IOException {
        // Mocking scan start failure
        org.apache.http.StatusLine statusLine = mock(org.apache.http.StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(httpResponse.getStatusLine()).thenReturn(statusLine); // Return the mocked StatusLine

        assertThrows(IOException.class, () -> debrickedService.startScan("uploadId", "testToken"));
    }
}
