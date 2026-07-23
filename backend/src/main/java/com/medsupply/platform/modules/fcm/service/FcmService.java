package com.medsupply.platform.modules.fcm.service;

public interface FcmService {

    /**
     * Sends a direct push notification to a specific device FCM registration token.
     */
    void sendNotification(String token, String title, String body);

    /**
     * Sends a push notification to all devices subscribed to a topic (e.g. B2C_CUSTOMERS, SALESMAN).
     */
    void sendTopicNotification(String topic, String title, String body);
}
