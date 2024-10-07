package com.example.ruleengine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    // Injecting the JavaMailSender to enable sending emails
    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends an email notification.
     *
     * @param to      The recipient's email address
     * @param subject The subject of the email
     * @param body    The body content of the email
     */
    public void sendEmail(String to, String subject, String body) {
        // Create a SimpleMailMessage object to construct the email
        SimpleMailMessage message = new SimpleMailMessage();

        // Set the recipient of the email
        message.setTo(to);

        // Set the subject of the email
        message.setSubject(subject);

        // Set the body text of the email
        message.setText(body);

        // Send the email using the mailSender
        mailSender.send(message);
    }
}
