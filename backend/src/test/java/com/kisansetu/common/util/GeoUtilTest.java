package com.kisansetu.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilTest {

    @Test
    void distanceKm_zeroForSamePoint() {
        assertEquals(0.0, GeoUtil.distanceKm(16.705, 74.2433, 16.705, 74.2433), 1e-6);
    }

    @Test
    void distanceKm_knownPair() {
        // Kolhapur (16.7050, 74.2433) to Kolhapur city centre (16.7000, 74.2400)
        double d = GeoUtil.distanceKm(16.7050, 74.2433, 16.7000, 74.2400);
        assertTrue(d > 0.4 && d < 1.0, "expected ~0.6 km, got " + d);
    }

    @Test
    void distanceKm_longerDistance() {
        // Kolhapur to Hyderabad ~ 490 km
        double d = GeoUtil.distanceKm(16.7050, 74.2433, 17.3850, 78.4867);
        assertTrue(d > 450 && d < 550, "expected ~490 km, got " + d);
    }

    @Test
    void asDouble_nullSafe() {
        assertNull(GeoUtil.asDouble(null));
        assertEquals(12.5, GeoUtil.asDouble(new BigDecimal("12.5")));
    }
}