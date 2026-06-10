package com.housing.servlet;

import com.housing.service.PredictionService;
import com.housing.service.SuggestionService;
import com.housing.util.ListingTypeUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class PredictServlet extends HttpServlet {
    private final PredictionService predictionService = new PredictionService();
    private final SuggestionService suggestionService = new SuggestionService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String listingType = ListingTypeUtil.normalize(req.getParameter("listingType"));
            if (listingType == null) {
                resp.getWriter().write("{\"success\":false,\"message\":\"请先在首页选择【租房】或【购房】\"}");
                return;
            }

            String district = req.getParameter("district");
            int year = Integer.parseInt(req.getParameter("year"));
            int month = Integer.parseInt(req.getParameter("month"));
            double area = Double.parseDouble(req.getParameter("area"));
            int floor = Integer.parseInt(req.getParameter("floor"));
            double distance = Double.parseDouble(req.getParameter("distanceToSubway"));
            double age = Double.parseDouble(req.getParameter("houseAge"));

            double unitPrice = predictionService.predictUnitPrice(
                    district, year, month, area, floor, distance, age, listingType);
            double totalPrice = unitPrice * area;

            Map<String, Double> impact = predictionService.factorImpact(area, floor, distance, age);
            String suggestion = suggestionService.buildDataDrivenSuggestion(
                    district, year, month, area, unitPrice, totalPrice, listingType);

            String unitLabel = ListingTypeUtil.isRent(listingType) ? "万/㎡/月" : "万/㎡";

            StringBuilder sb = new StringBuilder("{\"success\":true,");
            sb.append("\"listingType\":\"").append(listingType).append("\",");
            sb.append("\"listingTypeLabel\":\"").append(ListingTypeUtil.label(listingType)).append("\",");
            sb.append("\"unitPrice\":").append(String.format("%.2f", unitPrice)).append(",");
            sb.append("\"unitLabel\":\"").append(unitLabel).append("\",");
            sb.append("\"totalPrice\":").append(String.format("%.2f", totalPrice)).append(",");
            sb.append("\"suggestion\":\"").append(escapeJson(suggestion)).append("\",");
            sb.append("\"impact\":[");
            int idx = 0;
            for (Map.Entry<String, Double> en : impact.entrySet()) {
                if (idx++ > 0) sb.append(",");
                sb.append("{\"name\":\"").append(en.getKey()).append("\",\"value\":")
                        .append(String.format("%.2f", en.getValue())).append("}");
            }
            sb.append("]}");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.getWriter().write("{\"success\":false,\"message\":\"预测失败，请检查输入\"}");
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
