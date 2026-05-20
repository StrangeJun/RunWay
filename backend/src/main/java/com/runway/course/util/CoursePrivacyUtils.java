package com.runway.course.util;

import com.runway.course.dto.CoursePointResponse;

import java.util.ArrayList;
import java.util.List;

public final class CoursePrivacyUtils {

    public static final double PRIVACY_ZONE_METERS = 150.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private CoursePrivacyUtils() {}

    /**
     * Returns a new list omitting points within PRIVACY_ZONE_METERS of the start or end.
     * Cumulative path distance is used — not straight-line distance from endpoints.
     * Does not mutate DB data; call only when building API responses.
     */
    public static List<CoursePointResponse> maskPrivacyZone(List<CoursePointResponse> points) {
        if (points.size() < 2) return points;

        double[] cumFromStart = cumulativeDistances(points);
        double totalLength = cumFromStart[points.size() - 1];

        List<CoursePointResponse> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            double fromStart = cumFromStart[i];
            double fromEnd = totalLength - fromStart;
            if (fromStart >= PRIVACY_ZONE_METERS && fromEnd >= PRIVACY_ZONE_METERS) {
                result.add(points.get(i));
            }
        }
        return result;
    }

    /**
     * Rounds a coordinate to ~1.1 km precision (2 decimal places).
     * Use for start-point coordinates in public-facing responses (e.g. nearby list).
     */
    public static double maskCoordinate(double coord) {
        return Math.round(coord * 100.0) / 100.0;
    }

    private static double[] cumulativeDistances(List<CoursePointResponse> points) {
        double[] cum = new double[points.size()];
        cum[0] = 0;
        for (int i = 1; i < points.size(); i++) {
            cum[i] = cum[i - 1] + haversine(
                    points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude(),
                    points.get(i).getLatitude(),     points.get(i).getLongitude());
        }
        return cum;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
