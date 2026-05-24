package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.global.exception.CustomException;
import com.i_route.backend.gps.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoMobilityClient {

    private final RestTemplate restTemplate;

    @Value("${kakao.mobility.base-url}")
    private String baseUrl;

    @Value("${kakao.mobility.rest-api-key}")
    private String restApiKey;

    public Integer getDurationSeconds(
            Double originLat,
            Double originLng,
            RouteStop destinationStop,
            List<RouteStop> waypoints
    ) {
        String origin = originLng + "," + originLat;

        String destination =
                destinationStop.getLongitude()
                        + ","
                        + destinationStop.getLatitude();

        String waypointParam = toWaypointParam(waypoints);

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(baseUrl + "/v1/directions")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("priority", "TIME");

        if (!waypointParam.isBlank()) {
            builder.queryParam("waypoints", waypointParam);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        builder.build(false).toUriString(),
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

        Map body = response.getBody();

        if (body == null || body.get("routes") == null) {
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }

        List routes = (List) body.get("routes");

        if (routes.isEmpty()) {
            throw new CustomException(ErrorCode.KAKAO_ROUTE_NOT_FOUND);
        }

        Map firstRoute = (Map) routes.get(0);
        Map summary = (Map) firstRoute.get("summary");

        if (summary == null || summary.get("duration") == null) {
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }

        Object duration = summary.get("duration");

        return ((Number) duration).intValue();
    }

    private String toWaypointParam(List<RouteStop> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < waypoints.size(); i++) {
            RouteStop stop = waypoints.get(i);

            sb.append(stop.getLongitude())
                    .append(",")
                    .append(stop.getLatitude());

            if (i < waypoints.size() - 1) {
                sb.append("|");
            }
        }

        return sb.toString();
    }
}