package com.i_route.backend.gps.global.init;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.entity.OperationStatus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.driver.repository.DriverRepository;
import com.i_route.backend.gps.domain.route.entity.Route;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteRepository;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@Profile("local")
@RequiredArgsConstructor
public class GpsDummyDataInitializer implements CommandLineRunner {

    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusRepository busRepository;
    private final StudentRepository studentRepository;
    //private final UserRepository userRepository;

    @Override
    public void run(String... args) {
       /* if (userRepository.count() == 0) {

            userRepository.save(
                    User.builder()
                            .id(100L)
                            .email("parent100@test.com")
                            .emailVerified(true)
                            .loginType(LoginType.EMAIL)
                            .name("홍길동 학부모")
                            .nickname("parent100")
                            .phoneNumber("010-0000-0100")
                            .premiumCredits(0)
                            .role(Role.PARENT)
                            .username("parent100")
                            .build()
            );

            userRepository.save(
                    User.builder()
                            .id(101L)
                            .email("parent101@test.com")
                            .emailVerified(true)
                            .loginType(LoginType.EMAIL)
                            .name("김철수 학부모")
                            .nickname("parent101")
                            .phoneNumber("010-0000-0101")
                            .premiumCredits(0)
                            .role(Role.PARENT)
                            .username("parent101")
                            .build()
            );
        }*/

        if (busRepository.count() > 0) {
            return;
        }

        Driver driver = driverRepository.save(
                Driver.builder()
                        .name("김기사")
                        .phoneNumber("010-1111-2222")
                        .build()
        );

        Route route = routeRepository.save(
                Route.builder()
                        .routeName("영남대 A노선")
                        .build()
        );

        RouteStop stop1 = routeStopRepository.save(
                RouteStop.builder()
                        .routeId(route.getId())
                        .stopName("영남대 정문")
                        .latitude(35.8428)
                        .longitude(128.5586)
                        .stopOrder(1)
                        .build()
        );

        RouteStop stop2 = routeStopRepository.save(
                RouteStop.builder()
                        .routeId(route.getId())
                        .stopName("영남대 후문")
                        .latitude(35.8450)
                        .longitude(128.5630)
                        .stopOrder(2)
                        .build()
        );

        RouteStop stop3 = routeStopRepository.save(
                RouteStop.builder()
                        .routeId(route.getId())
                        .stopName("압량네거리")
                        .latitude(35.8500)
                        .longitude(128.5700)
                        .stopOrder(3)
                        .build()
        );

        Bus bus = busRepository.save(
                Bus.builder()
                        .busNumber("12가1234")
                        .driver(driver)
                        .route(route)
                        .operationStatus(OperationStatus.OPERATING)
                        .build()
        );

        studentRepository.save(
                Student.builder()
                        .busId(bus.getId())
                        .routeStopId(stop2.getId())
                        .name("홍길동")
                        .expectedDropOffTime(LocalTime.of(22, 0))
                        .parentId(100L)
                        .build()
        );

        studentRepository.save(
                Student.builder()
                        .busId(bus.getId())
                        .routeStopId(stop3.getId())
                        .name("김철수")
                        .expectedDropOffTime(LocalTime.of(22, 10))
                        .parentId(101L)
                        .build()
        );
    }

}
