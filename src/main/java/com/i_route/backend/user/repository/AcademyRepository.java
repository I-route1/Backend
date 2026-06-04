package com.i_route.backend.user.repository;


import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademyRepository extends JpaRepository<Academy, Long> {

    // 회원으로 학원 조회
    Optional<Academy> findByUser(User user);

    // 사업자 번호로 조회
    Optional<Academy> findByBusinessNumber(String businessNumber);

    // 사업자 번호 중복 확인
    boolean existsByBusinessNumber(String businessNumber);

    // 초대 코드로 조회
    Optional<Academy> findByInviteCode(String inviteCode);

    // 기사로 학원 조회
    Optional<Academy> findByDriver_Id(Long driverId);

    void deleteByUser_Id(Long userId);
}