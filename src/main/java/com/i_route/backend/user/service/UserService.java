package com.i_route.backend.user.service;

import com.i_route.backend.user.dto.DuplicateCheckRequest;
import com.i_route.backend.user.dto.DuplicateCheckResponse;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public DuplicateCheckResponse checkDuplicate(DuplicateCheckRequest request) {
        String cleanedPhone = request.getPhoneNumber() != null
                ? request.getPhoneNumber().replaceAll("\\D", "")
                : null;

        // DB 조회 (하나라도 겹치는게 있는지 확인)
        Optional<User> existingUserOpt = userRepository.findByEmailOrUsernameOrNicknameOrPhoneNumber(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                cleanedPhone
        );

        // 중복이 없는 경우
        if (existingUserOpt.isEmpty()) {
            return new DuplicateCheckResponse(false, List.of(), "사용 가능한 정보입니다.");
        }

        User existingUser = existingUserOpt.get();
        List<String> duplicates = new ArrayList<>();

        if (request.getUsername() != null && request.getUsername().equals(existingUser.getUsername())) {
            duplicates.add("아이디");
        }
        if (request.getNickname() != null && request.getNickname().equals(existingUser.getNickname())) {
            duplicates.add("닉네임");
        }
        if (cleanedPhone != null && cleanedPhone.equals(existingUser.getPhoneNumber())) {
            duplicates.add("휴대폰 번호");
        }

        if (request.getEmail() != null && request.getEmail().equals(existingUser.getEmail())) {
            duplicates.add("이메일");
        }
        String message = "이미 사용중인 " + String.join(", ", duplicates) + "입니다.";
        return new DuplicateCheckResponse(true, duplicates, message);
    }

    public DuplicateCheckResponse checkSingleField(String type, String value) {
        boolean isDuplicate = false;
        String fieldName = "";

        switch (type) {

            case "username":
                fieldName = "아이디";
                isDuplicate = userRepository.existsByUsername(value);
                break;

            case "nickname":
                fieldName = "닉네임";
                isDuplicate = userRepository.existsByNickname(value);
                break;

            case "email":
                fieldName = "이메일";
                isDuplicate = userRepository.existsByEmail(value);
                break;

            case "phoneNumber":
                fieldName = "휴대폰 번호";

                // ✅ 완전 정규화 (핵심)
                String cleanedPhone = value.replaceAll("\\D", "");

                isDuplicate = userRepository.existsByPhoneNumber(cleanedPhone);
                break;

            default:
                return null;
        }

        if (isDuplicate) {
            return new DuplicateCheckResponse(true, List.of(fieldName),
                    "이미 사용 중인 " + fieldName + "입니다.");
        }

        return new DuplicateCheckResponse(false, List.of(),
                "사용 가능한 " + fieldName + "입니다.");
    }
}