package com.i_route.backend.gps.domain.route.service;

import com.i_route.backend.gps.domain.gps.util.GeoUtil;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RouteProjectionCalculator {
    private static final double EARTH_RADIUS = 6371000.0;
    public record Projection(double progressMeters, double distanceFromRouteMeters, double[] stopDistances) {}

    public Projection project(double latitude, double longitude, List<RouteStop> stops) {
        if (stops == null || stops.size() < 2) throw new IllegalArgumentException("At least two stops required");
        double originLat = Math.toRadians(stops.get(0).getLatitude());
        double originLng = Math.toRadians(stops.get(0).getLongitude());
        double scale = Math.cos(originLat);
        double busX = EARTH_RADIUS * (Math.toRadians(longitude) - originLng) * scale;
        double busY = EARTH_RADIUS * (Math.toRadians(latitude) - originLat);
        int n = stops.size();
        double[] xs = new double[n], ys = new double[n], cumulative = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = EARTH_RADIUS * (Math.toRadians(stops.get(i).getLongitude()) - originLng) * scale;
            ys[i] = EARTH_RADIUS * (Math.toRadians(stops.get(i).getLatitude()) - originLat);
            if (i > 0) cumulative[i] = cumulative[i-1] + Math.hypot(xs[i]-xs[i-1], ys[i]-ys[i-1]);
        }
        double nearest = Double.POSITIVE_INFINITY, progress = 0;
        for (int i = 0; i < n-1; i++) {
            double dx = xs[i+1]-xs[i], dy = ys[i+1]-ys[i];
            double lengthSq = dx*dx + dy*dy;
            if (lengthSq < 1e-8) continue;
            double rawT = ((busX-xs[i])*dx + (busY-ys[i])*dy)/lengthSq;
            double t = Math.max(0, Math.min(1, rawT));
            if ((i == 0 && rawT < 0) || (i == n-2 && rawT > 1)) t = rawT;
            double distance = Math.hypot(busX-(xs[i]+t*dx), busY-(ys[i]+t*dy));
            if (distance < nearest) {
                nearest = distance;
                progress = cumulative[i] + t*Math.sqrt(lengthSq);
            }
        }
        if (!Double.isFinite(nearest)) throw new IllegalArgumentException("No nonzero route segments");
        return new Projection(progress, nearest, cumulative);
    }
}
