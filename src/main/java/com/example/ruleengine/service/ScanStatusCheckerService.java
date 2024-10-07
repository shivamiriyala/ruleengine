package com.example.ruleengine.service;

import com.example.ruleengine.entity.DependencyUpload;
import com.example.ruleengine.repository.DependencyUploadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ScanStatusCheckerService {

    // URL for Debricked API
    @Value("${debricked.api.url}")
    private String debrickedApiUrl;

    // Threshold for the number of vulnerabilities
    @Value("${debricked.api.vulnerabilities.threshold}")
    private int vulnerabilitiesThreshold;

    // API username for authentication
    @Value("${debricked.api.username}")
    private String apiUsername;

    // API password for authentication
    @Value("${debricked.api.password}")
    private String apiPassword;

    @Autowired
    private RestTemplate restTemplate; // REST client for making API calls

    @Autowired
    private DependencyUploadRepository repository; // Repository for accessing DependencyUpload data

    @Autowired
    private NotificationService notificationService; // Service for sending notifications

    // Scheduled method to check scan status every minute
    @Scheduled(fixedRate = 5000) // Check every minute
    public void checkScanStatus() {
        // Retrieve all uploads with IN_PROGRESS status
        List<DependencyUpload> uploads = repository.findAllByStatus(DependencyUpload.Status.IN_PROGRESS);
        System.out.println("checkScanStatus started ");

        // Set up HTTP headers for the API request
        HttpHeaders headers = new HttpHeaders();
        String token = null;
        try {
            // Obtain JWT token for authorization
            token = getJwtToken(apiUsername, apiPassword);
        } catch (IOException e) {
            throw new RuntimeException(e); // Rethrow exception if token retrieval fails
        }
        headers.set("Authorization", "Bearer "+token); // Set authorization header
        headers.set("accept", "*/*"); // Set accept header for response type

        // Create HTTP entity with the headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Loop through each upload and check its scan status
        for (DependencyUpload upload : uploads) {
            String ciUploadId = upload.getCiUploadId();

            // Make the GET request to check the scan status
            ResponseEntity<Map> response = restTemplate.exchange(debrickedApiUrl+"ci/upload/status?ciUploadId=" + ciUploadId,
                    HttpMethod.GET, entity, Map.class);

            // Check if the response is successful
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                int progress = (int) body.get("progress"); // Get progress percentage
                int vulnerabilitiesFound = (int) body.get("vulnerabilitiesFound"); // Get number of vulnerabilities found

                // If progress is 100%, update the upload status to COMPLETED
                if (progress == 100) {
                    upload.setStatus(DependencyUpload.Status.COMPLETED); // Update status to COMPLETED
                    repository.save(upload); // Save the updated status to the database

                    // If vulnerabilities exceed the threshold, send a notification
                    if (vulnerabilitiesFound > vulnerabilitiesThreshold) {
                        String email = upload.getEmail(); // Get the user's email
                        String subject = "Vulnerabilities Detected in Your Dependencies"; // Email subject
                        String message = createVulnerabilityReport(vulnerabilitiesFound); // Create message body

                        notificationService.sendEmail(email, subject, message); // Send email notification
                    }
                }
            } else {
                // Handle error response
                System.out.println("Error fetching scan status for ciUploadId: " + ciUploadId);
            }
        }
    }

    /**
     * Creates a report message based on the number of vulnerabilities found.
     *
     * @param vulnerabilitiesFound The number of vulnerabilities detected
     * @return A formatted report message
     */
    private String createVulnerabilityReport(int vulnerabilitiesFound) {
        return "A total of " + vulnerabilitiesFound + " vulnerabilities were detected in your dependencies.";
    }

    /**
     * Retrieves a JWT token for API authentication.
     *
     * @param username The API username
     * @param password The API password
     * @return The JWT token as a string
     * @throws IOException if the token retrieval fails
     */
    String getJwtToken(String username, String password) throws IOException {
        // Set up the POST request to obtain the JWT token
        HttpPost post = new HttpPost("https://debricked.com/api/login_check");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // Set up form data for the request
        StringEntity entity = new StringEntity("_username=" + username + "&_password=" + password);
        post.setEntity(entity);

        // Execute the request and obtain the response
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            // Check if the response is successful
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String jsonResponse = EntityUtils.toString(response.getEntity()); // Get the response body
                ObjectMapper mapper = new ObjectMapper(); // JSON mapper
                JsonNode node = mapper.readTree(jsonResponse); // Parse the JSON response
                return node.get("token").asText(); // Extract and return the token
            } else {
                throw new IOException("Failed to retrieve JWT token: " + response.getStatusLine().getReasonPhrase()); // Handle errors
            }
        }
    }
}
