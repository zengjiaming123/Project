package com.housing.dao;

import com.housing.model.HouseListing;
import com.housing.model.MarketSnapshot;
import com.housing.model.MarketStats;
import com.housing.util.ListingTypeUtil;

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
        appendListingType(sql, params, listingType);

        sql.append(" ORDER BY year DESC, month DESC, price ASC LIMIT 120");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    public List<double[]> trendByYear(String district, String listingType) throws Exception {
        StringBuilder sql = new StringBuilder(
                "SELECT year, AVG(price / area) avg_price FROM house_listings WHERE district = ?");
        List<Object> params = new ArrayList<>();
        params.add(district);
        appendListingType(sql, params, listingType);
        sql.append(" GROUP BY year ORDER BY year");
        return queryTrend(sql.toString(), params);
    }

    public List<double[]> trendByMonth(String district, int year, String listingType) throws Exception {
        StringBuilder sql = new StringBuilder(
                "SELECT month, AVG(price / area) avg_price FROM house_listings WHERE district = ? AND year = ?");
        List<Object> params = new ArrayList<>();
        params.add(district);
        params.add(year);
        appendListingType(sql, params, listingType);
        sql.append(" GROUP BY month ORDER BY month");
        return queryTrend(sql.toString(), params);
    }

    public List<HouseListing> fetchTrainingData(String listingType) throws Exception {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) throw new IllegalArgumentException("listingType 无效");

        List<HouseListing> results = new ArrayList<>();
        String sql = "SELECT district, year, month, price, area, floor, house_age, distance_to_subway, listing_type " +
                "FROM house_listings WHERE listing_type = ? AND area > 0 AND price > 0 ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    public List<HouseListing> fetchScatterSamples(String listingType, int limit) throws Exception {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) throw new IllegalArgumentException("listingType 无效");

        List<HouseListing> results = new ArrayList<>();
        String sql = "SELECT district, year, month, price, area, floor, house_age, distance_to_subway, listing_type " +
                "FROM house_listings WHERE listing_type = ? AND area > 0 AND price > 0 ORDER BY RAND() LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    public List<MarketSnapshot> fetchRandomMarketSnapshots(int limit) throws Exception {
        List<MarketSnapshot> results = new ArrayList<>();
        String sql = "SELECT district, year, month, listing_type, COUNT(*) AS cnt " +
                "FROM house_listings WHERE area > 0 AND price > 0 " +
                "GROUP BY district, year, month, listing_type HAVING cnt >= 3 " +
                "ORDER BY RAND() LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MarketSnapshot snapshot = new MarketSnapshot();
                    snapshot.setDistrict(rs.getString("district"));
                    snapshot.setYear(rs.getInt("year"));
                    snapshot.setMonth(rs.getInt("month"));
                    snapshot.setListingType(rs.getString("listing_type"));
                    snapshot.setSampleCount(rs.getInt("cnt"));
                    results.add(snapshot);
                }
            }
        }
        return results;
    }

    public MarketStats getMarketStats(String district, int year, int month, double area, String listingType) throws Exception {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) throw new IllegalArgumentException("listingType 无效");

        MarketStats stats = new MarketStats();
        StringBuilder sql = new StringBuilder(
                "SELECT AVG(price / area) AS avg_unit, AVG(price) AS avg_total, COUNT(*) AS cnt " +
                "FROM house_listings WHERE district = ? AND year = ? AND month = ? AND area > 0 AND listing_type = ?");
        List<Object> params = new ArrayList<>();
        params.add(district);
        params.add(year);
        params.add(month);
        params.add(type);

        if (area > 0) {
            sql.append(" AND area BETWEEN ? AND ?");
            params.add(area * 0.85);
            params.add(area * 1.15);
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setAvgUnitPrice(rs.getDouble("avg_unit"));
                    stats.setAvgTotalPrice(rs.getDouble("avg_total"));
                    stats.setSampleCount(rs.getInt("cnt"));
                }
            }
        }
        return stats;
    }

    private void appendListingType(StringBuilder sql, List<Object> params, String listingType) {
        String type = ListingTypeUtil.normalize(listingType);
        if (type != null) {
            sql.append(" AND listing_type = ?");
            params.add(type);
        }
    }

    private HouseListing mapRow(ResultSet rs) throws Exception {
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
        return item;
    }

    private List<double[]> queryTrend(String sql, List<Object> params) throws Exception {
        List<double[]> points = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) points.add(new double[]{rs.getDouble(1), rs.getDouble(2)});
            }
        }
        return points;
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }
}
