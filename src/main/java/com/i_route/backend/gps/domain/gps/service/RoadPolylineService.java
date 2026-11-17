package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RoadPolylineService {
    public record Point(double latitude, double longitude) {}
    private final RouteStopRepository stopsRepository;
    private final RestTemplate restTemplate;
    private final Map<Long, List<Point>> cache = new ConcurrentHashMap<>();
    @Value("${kakao.mobility.base-url:}") private String baseUrl;
    @Value("${kakao.mobility.rest-api-key:}") private String apiKey;

    public List<Point> getRoad(Long routeId) {
        return cache.computeIfAbsent(routeId, this::fetchRoad);
    }
    public void invalidate(Long routeId) { cache.remove(routeId); }

    @SuppressWarnings("unchecked")
    private List<Point> fetchRoad(Long routeId) {
        List<RouteStop> stops = stopsRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        if (stops.size() < 2) throw new IllegalStateException("At least two stops required");
        if (baseUrl.isBlank() || apiKey.isBlank()) throw new IllegalStateException("Kakao Mobility configuration missing");
        List<Point> result = new ArrayList<>();
        // Request adjacent legs to ensure every stop is traversed and avoid waypoint limits.
        for (int i = 0; i < stops.size() - 1; i++) {
            RouteStop a = stops.get(i), b = stops.get(i + 1);
            String uri = UriComponentsBuilder.fromUriString(baseUrl + "/v1/directions")
                    .queryParam("origin", a.getLongitude() + "," + a.getLatitude())
                    .queryParam("destination", b.getLongitude() + "," + b.getLatitude())
                    .queryParam("priority", "TIME")
                    .build().toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET,
                    new HttpEntity<Void>(headers), Map.class);
            Map<String,Object> body = response.getBody();
            if (body == null || !(body.get("routes") instanceof List<?> routes) || routes.isEmpty())
                throw new CustomException(ErrorCode.KAKAO_ROUTE_NOT_FOUND);
            Map<String,Object> first = (Map<String,Object>) routes.get(0);
            if (first.get("result_code") instanceof Number code && code.intValue() != 0)
                throw new CustomException(ErrorCode.KAKAO_ROUTE_NOT_FOUND);
            List<Map<String,Object>> sections = (List<Map<String,Object>>) first.get("sections");
            if (sections == null) throw new CustomException(ErrorCode.KAKAO_API_ERROR);
            int before = result.size();
            for (Map<String,Object> section : sections) {
                List<Map<String,Object>> roads = (List<Map<String,Object>>) section.get("roads");
                if (roads == null) continue;
                for (Map<String,Object> road : roads) {
                    List<Number> vertices = (List<Number>) road.get("vertexes");
                    if (vertices == null || vertices.size() % 2 != 0) continue;
                    for (int j = 0; j < vertices.size(); j += 2) {
                        Point pt = new Point(vertices.get(j+1).doubleValue(), vertices.get(j).doubleValue());
                        if (result.isEmpty() || !result.get(result.size()-1).equals(pt)) result.add(pt);
                    }
                }
            }
            if (result.size() == before) throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }
        return List.copyOf(result);
    }
}

