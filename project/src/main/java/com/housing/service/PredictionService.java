package com.housing.service;

import com.housing.dao.HouseListingDAO;
import com.housing.model.MarketStats;
import com.housing.util.ListingTypeUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class PredictionService {

    private static final Map<String, Double> SALE_DISTRICT_BASE = new LinkedHashMap<>();
    private static final Map<String, Double> RENT_DISTRICT_BASE = new LinkedHashMap<>();
    private final HouseListingDAO dao = new HouseListingDAO();

    static {
        SALE_DISTRICT_BASE.put("广州", 2.2);
        SALE_DISTRICT_BASE.put("深圳", 3.3);
        SALE_DISTRICT_BASE.put("珠海", 1.9);
        SALE_DISTRICT_BASE.put("佛山", 1.4);
        SALE_DISTRICT_BASE.put("东莞", 1.7);
        SALE_DISTRICT_BASE.put("中山", 1.3);
        SALE_DISTRICT_BASE.put("惠州", 1.2);
        SALE_DISTRICT_BASE.put("江门", 1.0);
        SALE_DISTRICT_BASE.put("肇庆", 0.9);

        RENT_DISTRICT_BASE.put("广州", 0.08);
        RENT_DISTRICT_BASE.put("深圳", 0.12);
        RENT_DISTRICT_BASE.put("珠海", 0.07);
        RENT_DISTRICT_BASE.put("佛山", 0.05);
        RENT_DISTRICT_BASE.put("东莞", 0.06);
        RENT_DISTRICT_BASE.put("中山", 0.05);
        RENT_DISTRICT_BASE.put("惠州", 0.045);
        RENT_DISTRICT_BASE.put("江门", 0.04);
        RENT_DISTRICT_BASE.put("肇庆", 0.038);
    }

    public double predictUnitPrice(String district, int year, int month, double area, int floor,
                                   double distance, double age, String listingType) throws Exception {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) throw new IllegalArgumentException("请先在首页选择租房或购房");

        double rulePrice = ListingTypeUtil.isRent(type)
                ? predictRentByRule(district, year, month, area, floor, distance, age)
                : predictSaleByRule(district, year, month, area, floor, distance, age);

        MarketStats stats = dao.getMarketStats(district, year, month, area, type);
        if (stats.hasData() && stats.getSampleCount() >= 3) {
            return rulePrice * 0.6 + stats.getAvgUnitPrice() * 0.4;
        }
        return rulePrice;
    }

    private double predictSaleByRule(String district, int year, int month, double area, int floor, double distance, double age) {
        double base = SALE_DISTRICT_BASE.getOrDefault(district, 1.2);
        return base * yearFactor(year) * monthFactor(month) * areaFactor(area)
                * floorFactor(floor) * subwayFactor(distance) * ageFactor(age);
    }

    private double predictRentByRule(String district, int year, int month, double area, int floor, double distance, double age) {
        double base = RENT_DISTRICT_BASE.getOrDefault(district, 0.06);
        return base * yearFactor(year) * monthFactor(month) * areaFactor(area)
                * floorFactor(floor) * subwayFactor(distance) * ageFactor(age);
    }

    private double yearFactor(int year) { return 1 + (year - 2023) * 0.035; }
    private double monthFactor(int month) { return 1 + (month - 6.5) * 0.003; }
    private double areaFactor(double area) { return area > 120 ? 0.96 : (area < 60 ? 1.08 : 1.0); }
    private double floorFactor(int floor) { return 1 + Math.min(floor, 33) * 0.002; }
    private double subwayFactor(double distance) { return 1 + Math.max(0, 2.0 - distance) * 0.035; }
    private double ageFactor(double age) { return 1 - Math.min(age, 40) * 0.004; }

    public Map<String, Double> factorImpact(double area, int floor, double distance, double age) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("面积影响", area > 120 ? -4.0 : (area < 60 ? 8.0 : 0.0));
        map.put("楼层影响", Math.min(floor, 33) * 0.2);
        map.put("地铁距离影响", Math.max(0, 2.0 - distance) * 3.5);
        map.put("房龄影响", -Math.min(age, 40) * 0.4);
        return map;
    }
}