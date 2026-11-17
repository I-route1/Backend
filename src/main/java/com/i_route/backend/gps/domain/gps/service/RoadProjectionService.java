package com.i_route.backend.gps.domain.gps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadProjectionService {
    private static final double R = 6371000.0;
    private final RoadPolylineService roads;
    public record Projection(double progressMeters, double offsetMeters, double totalMeters) {}

    public Projection project(Long routeId, double lat, double lon) {
        return project(roads.getRoad(routeId), lat, lon);
    }
    public Projection project(List<RoadPolylineService.Point> path, double lat, double lon) {
        if (path.size() < 2) throw new IllegalArgumentException("Polyline needs >=2 points");
        double originLat = Math.toRadians(path.get(0).latitude());
        double originLon = Math.toRadians(path.get(0).longitude());
        double scale = Math.cos(originLat);
        double bx = R * (Math.toRadians(lon)-originLon)*scale;
        double by = R * (Math.toRadians(lat)-originLat);
        double cumulative = 0, bestOffset = Double.POSITIVE_INFINITY, bestProgress = 0;
        for (int i=0; i<path.size()-1; i++) {
            var a=path.get(i); var b=path.get(i+1);
            double ax=R*(Math.toRadians(a.longitude())-originLon)*scale;
            double ay=R*(Math.toRadians(a.latitude())-originLat);
            double dx=R*(Math.toRadians(b.longitude()-a.longitude()))*scale;
            double dy=R*(Math.toRadians(b.latitude()-a.latitude()));
            double len2=dx*dx+dy*dy;
            if(len2 < 1e-8) continue;
            double t=Math.max(0,Math.min(1,((bx-ax)*dx+(by-ay)*dy)/len2));
            double offset=Math.hypot(bx-(ax+t*dx),by-(ay+t*dy));
            double len=Math.sqrt(len2);
            if(offset < bestOffset) { bestOffset=offset; bestProgress=cumulative+t*len; }
            cumulative+=len;
        }
        if(!Double.isFinite(bestOffset)) throw new IllegalArgumentException("Invalid polyline");
        return new Projection(bestProgress,bestOffset,cumulative);
    }
}

