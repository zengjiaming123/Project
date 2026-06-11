package com.housing.servlet;

import com.housing.service.NewsService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class NewsServlet extends HttpServlet {
    private final NewsService newsService = new NewsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        int count = 8;
        String countRaw = req.getParameter("count");
        if (countRaw != null && !countRaw.trim().isEmpty()) {
            try {
                count = Integer.parseInt(countRaw.trim());
            } catch (NumberFormatException ignored) {
                count = 8;
            }
        }

        try {
            List<String> items = newsService.buildNewsItems(count);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"success\":true,\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(items.get(i))).append("\"");
            }
            sb.append("]}");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.getWriter().write("{\"success\":false,\"message\":\"资讯生成失败\"}");
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
