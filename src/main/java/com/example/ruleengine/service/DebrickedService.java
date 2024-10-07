package com.example.ruleengine.service;

import com.example.ruleengine.entity.DependencyUpload;
import com.example.ruleengine.repository.DependencyUploadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

@Service
public class DebrickedService {

    // URL of the Debricked API, loaded from application properties
    @Value("${debricked.api.url}")
    private String debrickedApiUrl;

    // Username for API access, loaded from application properties
    @Value("${debricked.api.username}")
    private String apiUsername;

    // Password for API access, loaded from application properties
    @Value("${debricked.api.password}")
    private String apiPassword;

    @Autowired
    private NotificationService notificationService; // Service for sending notifications

    @Autowired
    private DependencyUploadRepository repository; // Repository for dependency uploads

    /**
     * Uploads dependency files to Debricked API and initiates a scan.
     * @param files Array of files to be uploaded
     * @param email Email address for notifications
     * @throws IOException If any I/O errors occur
     */
    public boolean uploadDependencies(MultipartFile[] files, String email) throws IOException {
        String uploadId = null;
        boolean retval = false;

        String filednames[];


        // Step 1: Get JWT token based on username and password
        String token = getJwtToken(apiUsername, apiPassword);

        // Step 2: Upload the files
        for (MultipartFile file : files) {
            if (uploadId == null) {
                uploadId = uploadFileToDebricked(file, email, token, uploadId);
                if (uploadId != null) {
                    retval = true;
                }
            } else {
                uploadId = uploadFileToDebricked(file, email, token, uploadId);
            }

        }



        // Step 3: Start scan if upload is successful/partially successful
        if (uploadId != null) {
            startScan(uploadId, token);
            // Save upload info in the database with IN_PROGRESS status
            DependencyUpload upload = new DependencyUpload();
            upload.setCiUploadId(uploadId);
            upload.setEmail(email);
            upload.setStatus(DependencyUpload.Status.IN_PROGRESS); // Set status to IN_PROGRESS
            repository.save(upload);
        }

       return retval;
    }

    /**
     * Uploads a single file to Debricked API.
     * @param file The file to upload
     * @param email Email address for notifications
     * @param token JWT token for authentication
     * @return The upload ID if successful, null otherwise
     * @throws IOException If any I/O errors occur
     */
    private String uploadFileToDebricked(MultipartFile file, String email, String token, String ciUploadID) throws IOException {
        // Create a POST request for uploading dependencies
        HttpPost post = new HttpPost(debrickedApiUrl + "uploads/dependencies/files");
        post.addHeader("Authorization", "Bearer " + token);
        post.addHeader("accept", "*/*");  // Add the accept header

        // Build the multipart entity with file data
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("fileData", file.getInputStream(), ContentType.DEFAULT_BINARY, file.getOriginalFilename());  // Proper binary body for file upload
        builder.addTextBody("commitName", "commit6");  // Sample commit name
        builder.addTextBody("repositoryName", "repo6"); // Sample repository name
        if (ciUploadID != null) {
            builder.addTextBody("ciUploadId", ciUploadID);
        }


        post.setEntity(builder.build());  // Set the entity in the request

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Parse the response and return uploadId
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonResponse);
                return node.get("ciUploadId").asText(); // Extract ciUploadId from the response
            } else {
                // Notify user if file upload fails
                notificationService.sendEmail(email, "Failed to upload file ", file.getOriginalFilename());
                return null;
            }
        }
    }

    /**
     * Starts the scan for the uploaded file.
     * @param ciUploadId The ID of the upload
     * @param token JWT token for authentication
     * @throws IOException If any I/O errors occur
     */
    protected void startScan(String ciUploadId, String token) throws IOException {
        // Create a POST request to start the scan
        HttpPost post = new HttpPost(debrickedApiUrl + "finishes/dependencies/files/uploads");
        post.addHeader("Authorization", "Bearer " + token);
        post.addHeader("accept", "application/json");

        // Build the entity with necessary parameters
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("ciUploadId", ciUploadId, ContentType.TEXT_PLAIN);
        builder.addTextBody("returnCommitData", "false", ContentType.TEXT_PLAIN); // Do not return commit data

        post.setEntity(builder.build());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Failed to start scan: " + response.getStatusLine().getReasonPhrase());
            }
        }
    }

    /**
     * Retrieves a JWT token for authentication.
     * @param username The username for the API
     * @param password The password for the API
     * @return The JWT token
     * @throws IOException If any I/O errors occur
     */
    protected String getJwtToken(String username, String password) throws IOException {
        // Create a POST request for authentication
        HttpPost post = new HttpPost("https://debricked.com/api/login_check");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // Set up form data
        StringEntity entity = new StringEntity("_username=" + username + "&_password=" + password);
        post.setEntity(entity);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Parse response and return token
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonResponse);
                return node.get("token").asText(); // Extract token from response
            } else {
                throw new IOException("Failed to retrieve JWT token: " + response.getStatusLine().getReasonPhrase());
            }
        }
    }
}
