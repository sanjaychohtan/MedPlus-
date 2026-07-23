package com.medsupply.platform.modules.fcm.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.medsupply.platform.modules.fcm.service.FcmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmServiceImpl implements FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmServiceImpl.class);

    private boolean isFirebaseEnabled() {
        return !FirebaseApp.getApps().isEmpty();
    }

    @Override
    public void sendNotification(String token, String title, String body) {
        if (!isFirebaseEnabled()) {
            log.info("[MOCK FCM PUSH] TargetToken: {}, Title: {}, Body: {}", token, title, body);
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully dispatched FCM direct push notification. MsgId: {}", messageId);
        } catch (Exception e) {
            log.error("Failed to transmit direct FCM notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendTopicNotification(String topic, String title, String body) {
        if (!isFirebaseEnabled()) {
            log.info("[MOCK FCM TOPIC PUSH] Topic: {}, Title: {}, Body: {}", topic, title, body);
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(notification)
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully dispatched FCM topic push notification to /topics/{}. MsgId: {}", topic, messageId);
        } catch (Exception e) {
            log.error("Failed to transmit topic FCM notification: {}", e.getMessage(), e);
        }
    }
}
