package com.housing.service;

import java.util.LinkedHashMap;
import java.util.Map;

public class PredictionService {

    private static final Map<String, Double> DISTRICT_BASE = new LinkedHashMap<>();
    static {
        DISTRICT_BASE.put("广州", 2.2);
        DISTRICT_BASE.put("深圳", 3.3);
        DISTRICT_BASE.put("珠海", 1.9);
        DISTRICT_BASE.put("佛山", 1.4);
        DISTRICT_BASE.put("东莞", 1.7);
        DISTRICT_BASE.put("中山", 1.3);
        DISTRICT_BASE.put("惠州", 1.2);
        DISTRICT_BASE.put("江门", 1.0);
        DISTRICT_BASE.put("肇庆", 0.9);
    }

    public double predictUnitPrice(String district, int year, int month, double area, int floor, double distance, double age) {
        double districtBase = DISTRICT_BASE.getOrDefault(district, 1.2);
        double yearFactor = 1 + (year - 2023) * 0.035;
        double monthFactor = 1 + (month - 6.5) * 0.003;
        double areaFactor = area > 120 ? 0.96 : (area < 60 ? 1.08 : 1.0);
        double floorFactor = 1 + Math.min(floor, 33) * 0.002;
        double subwayFactor = 1 + Math.max(0, 2.0 - distance) * 0.035;
        double ageFactor = 1 - Math.min(age, 40) * 0.004;
        return districtBase * yearFactor * monthFactor * areaFactor * floorFactor * subwayFactor * ageFactor;
    }

    public Map<String, Double> factorImpact(double area, int floor, double distance, double age) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("面积影响", area > 120 ? -4.0 : (area < 60 ? 8.0 : 0.0));
        map.put("楼层影响", Math.min(floor, 33) * 0.2);
        map.put("地铁距离影响", Math.max(0, 2.0 - distance) * 3.5);
        map.put("房龄影响", -Math.min(age, 40) * 0.4);
        return map;
    }
}