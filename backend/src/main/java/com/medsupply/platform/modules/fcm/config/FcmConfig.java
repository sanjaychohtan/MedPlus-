package com.medsupply.platform.modules.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    @Value("${app.firebase.fcm.service-account-json-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initializeFirebase() {
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            log.warn("Firebase config path is empty. Firebase Cloud Messaging initialized in MOCK/STUB mode.");
            return;
        }

        try {
            File configFile = new File(serviceAccountPath);
            if (!configFile.exists()) {
                log.warn("Firebase configuration file not found at: {}. FCM will operate in MOCK/STUB mode.", serviceAccountPath);
                return;
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(configFile);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Cloud Messaging has been successfully initialized from custom service account configuration.");
            }
        } catch (IOException e) {
            log.error("Firebase Cloud Messaging initialization failed: {}", e.getMessage(), e);
        }
    }
}
