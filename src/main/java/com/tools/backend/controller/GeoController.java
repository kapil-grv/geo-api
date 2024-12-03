package com.tools.backend.controller;

import ch.hsr.geohash.GeoHash;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final H3Core h3;

    public GeoController() throws IOException {
        this.h3 = H3Core.newInstance();
    }

    // GeoHash Endpoints
    @PostMapping("/geohash")
    public List<Map<String, String>> getGeohash(@RequestBody List<GeohashRequest> requests) {
        return requests.stream()
                .map(request -> Map.of(
                        "latitude", String.valueOf(request.lat),
                        "longitude", String.valueOf(request.lon),
                        "precision", String.valueOf(request.precision),
                        "geohash", GeoHash.withCharacterPrecision(request.lat, request.lon, request.precision).toBase32()
                ))
                .collect(Collectors.toList());
    }

    @PostMapping("/decode-geohash")
    public List<Map<String, Object>> decodeGeohash(@RequestBody List<String> geohashes) {
        return geohashes.stream()
                .map(geohash -> {
                    GeoHash decoded = GeoHash.fromGeohashString(geohash);
                    Map<String, Object> result = new HashMap<>();
                    result.put("geohash", geohash);
                    result.put("latitude", decoded.getBoundingBox().getCenter().getLatitude());
                    result.put("longitude", decoded.getBoundingBox().getCenter().getLongitude());
                    result.put("precision", geohash.length());
                    return result;
                })
                .collect(Collectors.toList());
    }

    // H3 Endpoints
    @PostMapping("/h3")
    public List<Map<String, String>> getH3(@RequestBody List<H3Request> requests) {
        return requests.stream()
                .map(request -> Map.of(
                        "latitude", String.valueOf(request.lat),
                        "longitude", String.valueOf(request.lon),
                        "resolution", String.valueOf(request.resolution),
                        "h3Index", h3.latLngToCellAddress(request.lat, request.lon, request.resolution)
                ))
                .collect(Collectors.toList());
    }

    @PostMapping("/decode-h3")
    public List<Map<String, Object>> decodeH3(@RequestBody List<String> h3Indices) {
        return h3Indices.stream()
                .map(h3Index -> {
                    LatLng latLon = h3.cellToLatLng(h3Index);
                    Map<String, Object> result = new HashMap<>();
                    result.put("h3Index", h3Index);
                    result.put("latitude", latLon.lat);
                    result.put("longitude", latLon.lng);
                    result.put("precision", h3.getResolution(h3Index));
                    return result;
                })
                .collect(Collectors.toList());
    }

    // S2 Endpoints
    @PostMapping("/s2")
    public List<Map<String, String>> getS2CellId(@RequestBody List<S2Request> requests) {
        return requests.stream()
                .map(request -> {
                    S2LatLng latLng = S2LatLng.fromDegrees(request.lat, request.lon);
                    S2CellId cellId = S2CellId.fromLatLng(latLng);
                    return Map.of(
                            "latitude", String.valueOf(request.lat),
                            "longitude", String.valueOf(request.lon),
                            "s2CellId", cellId.toToken()
                    );
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/decode-s2")
    public List<Map<String, Object>> decodeS2CellId(@RequestBody List<String> s2CellIds) {
        return s2CellIds.stream()
                .map(cellIdStr -> {
                    S2CellId cellId = S2CellId.fromToken(cellIdStr);
                    S2LatLng latLng = cellId.toLatLng();
                    Map<String, Object> result = new HashMap<>();
                    result.put("s2CellId", cellIdStr);
                    result.put("latitude", latLng.latDegrees());
                    result.put("longitude", latLng.lngDegrees());
                    return result;
                })
                .collect(Collectors.toList());
    }

    // DTOs
    public static class GeohashRequest {
        public double lat;
        public double lon;
        public int precision;
    }

    public static class H3Request {
        public double lat;
        public double lon;
        public int resolution;
    }

    public static class S2Request {
        public double lat;
        public double lon;
    }
}
