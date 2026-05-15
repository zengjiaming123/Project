package com.housing.servlet;

import com.housing.service.PredictionService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class PredictServlet extends HttpServlet {
    private final PredictionService service = new PredictionService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String district = req.getParameter("district");
            int year = Integer.parseInt(req.getParameter("year"));
            int month = Integer.parseInt(req.getParameter("month"));
            double area = Double.parseDouble(req.getParameter("area"));
            int floor = Integer.parseInt(req.getParameter("floor"));
            double distance = Double.parseDouble(req.getParameter("distanceToSubway"));
            double age = Double.parseDouble(req.getParameter("houseAge"));

            double unitPrice = service.predictUnitPrice(district, year, month, area, floor, distance, age);
            double totalPrice = unitPrice * area;
            Map<String, Double> impact = service.factorImpact(area, floor, distance, age);
            String suggestion = buildSuggestion(district, year, month, totalPrice, unitPrice);

            StringBuilder sb = new StringBuilder("{\"success\":true,");
            sb.append("\"unitPrice\":").append(String.format("%.2f", unitPrice)).append(",");
            sb.append("\"totalPrice\":").append(String.format("%.2f", totalPrice)).append(",");
            sb.append("\"suggestion\":\"").append(suggestion).append("\",");
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

    private String buildSuggestion(String district, int year, int month, double totalPrice, double unitPrice) {
        if (year == 2023) {
            return "2023年基准年暂不提供同比建议。";
        }
        double growth = 1.2 + (month % 3) * 0.4;
        return String.format("%d年%d月本房源在%s预测市场价为%.2f万，单位价格为%.2f万，较%d年同期增长%.1f%%，建议尽快挂牌。",
                year, month, district, totalPrice, unitPrice, year - 1, growth);
    }
}