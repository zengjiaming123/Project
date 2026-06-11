package com.housing.service;

import com.housing.dao.HouseListingDAO;
import com.housing.model.MarketSnapshot;
import com.housing.model.MarketStats;
import com.housing.util.ListingTypeUtil;

import java.util.ArrayList;
import java.util.List;

public class NewsService {

    private static final int MIN_SAMPLES = 3;
    private final HouseListingDAO dao = new HouseListingDAO();

    public List<String> buildNewsItems(int count) throws Exception {
        int target = Math.max(1, Math.min(count, 20));
        List<MarketSnapshot> snapshots = dao.fetchRandomMarketSnapshots(target * 3);
        List<String> items = new ArrayList<>();

        for (MarketSnapshot snapshot : snapshots) {
            if (items.size() >= target) break;
            String text = formatSnapshot(snapshot);
            if (text != null) items.add(text);
        }

        while (items.size() < target) {
            items.add("系统提示：使用【房价预测】前请先在首页选择租房或购房模式");
        }
        return items;
    }

    private String formatSnapshot(MarketSnapshot snapshot) throws Exception {
        String type = ListingTypeUtil.normalize(snapshot.getListingType());
        if (type == null) return null;

        MarketStats current = dao.getMarketStats(
                snapshot.getDistrict(), snapshot.getYear(), snapshot.getMonth(), 0, type);
        if (!current.hasData() || current.getSampleCount() < MIN_SAMPLES) return null;

        boolean isRent = ListingTypeUtil.isRent(type);
        String typeLabel = ListingTypeUtil.label(type);
        String unitLabel = isRent ? "万/㎡/月" : "万/㎡";
        String district = snapshot.getDistrict();
        int year = snapshot.getYear();
        int month = snapshot.getMonth();

        if (year <= 2023) {
            return String.format("%s %d年%d月【%s】均价约 %.2f%s（样本%d条），2023年及以前为基准期，暂无同比",
                    district, year, month, typeLabel, current.getAvgUnitPrice(), unitLabel, current.getSampleCount());
        }

        MarketStats lastYear = dao.getMarketStats(district, year - 1, month, 0, type);
        if (!lastYear.hasData() || lastYear.getSampleCount() < MIN_SAMPLES) {
            return String.format("%s %d年%d月【%s】均价约 %.2f%s（样本%d条），缺少%d年同月历史样本",
                    district, year, month, typeLabel, current.getAvgUnitPrice(), unitLabel,
                    current.getSampleCount(), year - 1);
        }

        double yoy = (lastYear.getAvgUnitPrice() > 0)
                ? (current.getAvgUnitPrice() - lastYear.getAvgUnitPrice()) / lastYear.getAvgUnitPrice() * 100.0
                : 0.0;

        return String.format("%s %d年%d月【%s】均价约 %.2f%s，同比%s %.1f%%（样本%d条）",
                district, year, month, typeLabel, current.getAvgUnitPrice(), unitLabel,
                yoy >= 0 ? "上涨" : "下降", Math.abs(yoy), current.getSampleCount());
    }
}
