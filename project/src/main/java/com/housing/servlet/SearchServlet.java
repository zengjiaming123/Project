package com.housing.servlet;

import com.housing.dao.HouseListingDAO;
import com.housing.model.HouseListing;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class SearchServlet extends HttpServlet {
    private final HouseListingDAO dao = new HouseListingDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String district = req.getParameter("district");
            Integer year = parseInt(req.getParameter("year"));
            Integer month = parseInt(req.getParameter("month"));
            Double budget = parseDouble(req.getParameter("budget"));
            Double area = parseDouble(req.getParameter("area"));
            Integer floor = parseInt(req.getParameter("floor"));
            Double distance = parseDouble(req.getParameter("distanceToSubway"));
            Double houseAge = parseDouble(req.getParameter("houseAge"));
            String listingType = req.getParameter("listingType");

            List<HouseListing> list = dao.search(district, year, month, budget, area, floor, distance, houseAge, listingType);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"success\":true,\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                HouseListing h = list.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"district\":\"").append(h.getDistrict()).append("\",")
                        .append("\"year\":").append(h.getYear()).append(",")
                        .append("\"month\":").append(h.getMonth()).append(",")
                        .append("\"price\":").append(String.format("%.2f", h.getPrice())).append(",")
                        .append("\"area\":").append(String.format("%.2f", h.getArea())).append(",")
                        .append("\"floor\":").append(h.getFloor())
                        .append("}");
            }
            sb.append("]}");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.getWriter().write("{\"success\":false,\"message\":\"搜索失败: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private Integer parseInt(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        return Integer.parseInt(v.trim());
    }

    private Double parseDouble(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        return Double.parseDouble(v.trim());
    }
}