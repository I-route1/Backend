package com.i_route.backend.gps.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.i_route.backend.gps.domain.notification.entity.FcmToken;
import com.i_route.backend.gps.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FirebasePushService {

    private final FirebaseApp firebaseApp;
    private final FcmTokenRepository fcmTokenRepository;

    public void sendToUser(
            Long userId,
            String title,
            String body
    ) {
        List<FcmToken> tokens =
                fcmTokenRepository.findByUserId(userId);

        for (FcmToken fcmToken : tokens) {

            Message message = Message.builder()
                    .setToken(fcmToken.getToken())
                    .setNotification(
                            Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .build();

            try {
                FirebaseMessaging.getInstance(firebaseApp)
                        .send(message);

            } catch (FirebaseMessagingException e) {
                System.err.println(
                        "FCM 전송 실패 userId="
                                + userId
                                + ", error=" + e.getMessage()
                );
            }
        }
    }
}
