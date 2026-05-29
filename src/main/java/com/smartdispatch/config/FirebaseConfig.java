package com.smartdispatch.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = getClass().getResourceAsStream("/firebase-service-account.json");
            
            // If the file is not there, we don't want to crash the whole app in dev
            if (serviceAccount == null) {
                System.out.println("⚠️ firebase-service-account.json not found in resources. Firebase Push is disabled.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase Admin SDK initialized successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
