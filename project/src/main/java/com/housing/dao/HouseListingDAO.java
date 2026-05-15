package com.housing.dao;

import com.housing.model.HouseListing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class HouseListingDAO {

    public List<HouseListing> search(String district, Integer year, Integer month, Double budget, Double area,
                                     Integer floor, Double distanceToSubway, Double houseAge, String listingType) throws Exception {
        List<HouseListing> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT district, year, month, price, area, floor, house_age, distance_to_subway, listing_type FROM house_listings WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (district != null && !district.isEmpty()) { sql.append(" AND district = ?"); params.add(district); }
        if (year != null) { sql.append(" AND year = ?"); params.add(year); }
        if (month != null) { sql.append(" AND month = ?"); params.add(month); }
        if (budget != null) { sql.append(" AND price <= ?"); params.add(budget); }
        if (area != null) { sql.append(" AND area >= ?"); params.add(area); }
        if (floor != null) { sql.append(" AND floor = ?"); params.add(floor); }
        if (distanceToSubway != null) { sql.append(" AND distance_to_subway <= ?"); params.add(distanceToSubway); }
        if (houseAge != null) { sql.append(" AND house_age <= ?"); params.add(houseAge); }
        if (listingType != null && !listingType.isEmpty()) { sql.append(" AND listing_type = ?"); params.add(listingType); }

        sql.append(" ORDER BY year DESC, month DESC, price ASC LIMIT 120");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HouseListing item = new HouseListing();
                    item.setDistrict(rs.getString("district"));
                    item.setYear(rs.getInt("year"));
                    item.setMonth(rs.getInt("month"));
                    item.setPrice(rs.getDouble("price"));
                    item.setArea(rs.getDouble("area"));
                    item.setFloor(rs.getInt("floor"));
                    item.setHouseAge(rs.getDouble("house_age"));
                    item.setDistanceToSubway(rs.getDouble("distance_to_subway"));
                    item.setListingType(rs.getString("listing_type"));
                    results.add(item);
                }
            }
        }
        return results;
    }

    public List<double[]> trendByYear(String district) throws Exception {
        String sql = "SELECT year, AVG(price / area) avg_price FROM house_listings WHERE district = ? GROUP BY year ORDER BY year";
        return queryTrend(sql, district);
    }

    public List<double[]> trendByMonth(String district, int year) throws Exception {
        String sql = "SELECT month, AVG(price / area) avg_price FROM house_listings WHERE district = ? AND year = ? GROUP BY month ORDER BY month";
        return queryTrend(sql, district, year);
    }

    private List<double[]> queryTrend(String sql, Object... params) throws Exception {
        List<double[]> points = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    points.add(new double[]{rs.getDouble(1), rs.getDouble(2)});
                }
            }
        }
        return points;
    }
}