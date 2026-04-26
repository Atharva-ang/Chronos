package com.chronos.worker_engine_chronos.service;

import com.chronos.worker_engine_chronos.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Email notification service for sending job failure notifications
 * This is a placeholder implementation. To enable actual email sending:
 *
 * 1. Add spring-boot-starter-mail dependency to pom.xml:
 *    <dependency>
 *        <groupId>org.springframework.boot</groupId>
 *        <artifactId>spring-boot-starter-mail</artifactId>
 *    </dependency>
 *
 * 2. Configure SMTP in application.properties:
 *    spring.mail.host=smtp.gmail.com
 *    spring.mail.port=587
 *    spring.mail.username=your-email@gmail.com
 *    spring.mail.password=your-app-password
 *    spring.mail.properties.mail.smtp.auth=true
 *    spring.mail.properties.mail.smtp.starttls.enable=true
 *
 * 3. Inject JavaMailSender and uncomment the sendEmail method
 */
@Slf4j
@Service
public class EmailNotificationService {

    @Value("${chronos.notification.from-email:noreply@chronos.com}")
    private String fromEmail;

    @Value("${chronos.notification.enabled:false}")
    private boolean notificationsEnabled;

    // Uncomment when spring-boot-starter-mail is added
    // private final JavaMailSender mailSender;
    //
    // public EmailNotificationService(JavaMailSender mailSender) {
    //     this.mailSender = mailSender;
    // }

    /**
     * Send job failure notification email
     */
    public void sendJobFailureEmail(Job job, String errorMessage) {
        if (!notificationsEnabled) {
            log.debug("Email notifications disabled. Would have sent failure notification for job {}", job.getId());
            return;
        }

        String userEmail = job.getUsername(); // Assuming username is email
        String subject = String.format("Job Failed: %s (ID: %d)", job.getJobType(), job.getId());
        String body = buildFailureEmailBody(job, errorMessage);

        sendEmail(userEmail, subject, body);
    }

    /**
     * Build email body for job failure
     */
    private String buildFailureEmailBody(Job job, String errorMessage) {
        return String.format("""
                Dear User,

                Your scheduled job has failed after %d retry attempts.

                Job Details:
                - Job ID: %d
                - Job Type: %s
                - Created At: %s
                - Max Retries: %d

                Error Message:
                %s

                Please review your job configuration and try again.

                Best regards,
                Chronos Job Scheduler
                """,
                job.getRetryCount(),
                job.getId(),
                job.getJobType(),
                job.getCreatedAt(),
                job.getMaxRetries(),
                errorMessage
        );
    }

    /**
     * Send email using JavaMailSender
     * Currently logs only - uncomment implementation when mail dependency is added
     */
    private void sendEmail(String to, String subject, String body) {
        log.info("Email Notification:");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);

        // Uncomment when spring-boot-starter-mail is added:
        // try {
        //     SimpleMailMessage message = new SimpleMailMessage();
        //     message.setFrom(fromEmail);
        //     message.setTo(to);
        //     message.setSubject(subject);
        //     message.setText(body);
        //     mailSender.send(message);
        //     log.info("Email sent successfully to {}", to);
        // } catch (Exception e) {
        //     log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        // }
    }
}
