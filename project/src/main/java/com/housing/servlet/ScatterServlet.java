package com.housing.servlet;

import com.housing.dao.HouseListingDAO;
import com.housing.model.HouseListing;
import com.housing.util.ListingTypeUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ScatterServlet extends HttpServlet {

    private final HouseListingDAO dao = new HouseListingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String listingType = ListingTypeUtil.normalize(req.getParameter("listingType"));
            if (listingType == null) {
                resp.getWriter().write("{\"success\":false,\"message\":\"请先在首页选择租房或购房\"}");
                return;
            }

            List<HouseListing> samples = dao.fetchScatterSamples(listingType, 200);
            StringBuilder sb = new StringBuilder("{\"success\":true,\"points\":[");
            for (int i = 0; i < samples.size(); i++) {
                HouseListing item = samples.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"area\":").append(item.getArea()).append(",")
                        .append("\"floor\":").append(item.getFloor()).append(",")
                        .append("\"distance\":").append(item.getDistanceToSubway()).append(",")
                        .append("\"age\":").append(item.getHouseAge()).append(",")
                        .append("\"price\":").append(item.getPrice())
                        .append("}");
            }
            sb.append("]}");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.getWriter().write("{\"success\":false,\"message\":\"散点数据加载失败\"}");
        }
    }
}
