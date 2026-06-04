package com.housing.servlet;

import com.housing.dao.HouseListingDAO;
import com.housing.util.ListingTypeUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class TrendServlet extends HttpServlet {
    private final HouseListingDAO dao = new HouseListingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        String district = req.getParameter("district");
        String yearRaw = req.getParameter("year");
        String listingType = ListingTypeUtil.normalize(req.getParameter("listingType"));

        if (district == null || district.trim().isEmpty()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"地区为必选\"}");
            return;
        }
        if (listingType == null) {
            resp.getWriter().write("{\"success\":false,\"message\":\"请先在首页选择【租房】或【购房】\"}");
            return;
        }

        try {
            List<double[]> points = (yearRaw == null || yearRaw.trim().isEmpty())
                    ? dao.trendByYear(district, listingType)
                    : dao.trendByMonth(district, Integer.parseInt(yearRaw), listingType);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"success\":true,\"mode\":\"")
                    .append((yearRaw == null || yearRaw.trim().isEmpty()) ? "year" : "month")
                    .append("\",\"listingType\":\"").append(listingType).append("\",")
                    .append("\"points\":[");
            for (int i = 0; i < points.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("[").append((int) points.get(i)[0]).append(",")
                        .append(String.format("%.2f", points.get(i)[1])).append("]");
            }
            sb.append("]}");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.getWriter().write("{\"success\":false,\"message\":\"趋势查询失败\"}");
        }
    }
}
