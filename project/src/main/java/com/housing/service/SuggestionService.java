package com.housing.service;

import com.housing.dao.HouseListingDAO;
import com.housing.model.MarketStats;
import com.housing.util.ListingTypeUtil;

public class SuggestionService {

    private static final int MIN_SAMPLES = 3;
    private final HouseListingDAO dao = new HouseListingDAO();

    public String buildDataDrivenSuggestion(String district, int year, int month, double area,
                                            double predictUnitPrice, double predictTotalPrice,
                                            String listingType) {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) return "请返回首页先选择【租房】或【购房】后再进行预测。";

        try {
            boolean isRent = ListingTypeUtil.isRent(type);
            String typeLabel = ListingTypeUtil.label(type);
            MarketStats current = fetchStatsWithFallback(district, year, month, area, type);

            if (!current.hasData() || current.getSampleCount() < MIN_SAMPLES) {
                return String.format("%d年%d月【%s】在%s暂无足够%s样本（需至少%d条），请先补全对应类型数据。",
                        year, month, typeLabel, district, typeLabel, MIN_SAMPLES);
            }

            if (year <= 2023) {
                return baseLine(year, month, district, typeLabel, isRent, predictTotalPrice, predictUnitPrice)
                        + "2023年及以前为基准期，暂不提供同比建议。";
            }

            MarketStats lastYear = fetchStatsWithFallback(district, year - 1, month, area, type);
            if (!lastYear.hasData() || lastYear.getSampleCount() < MIN_SAMPLES) {
                return baseLine(year, month, district, typeLabel, isRent, predictTotalPrice, predictUnitPrice)
                        + String.format("缺少%d年同月%s历史样本，暂不提供同比建议。", year - 1, typeLabel);
            }

            double yoyGrowth = (lastYear.getAvgUnitPrice() > 0)
                    ? (current.getAvgUnitPrice() - lastYear.getAvgUnitPrice()) / lastYear.getAvgUnitPrice() * 100.0 : 0.0;

            String action = decideAction(predictUnitPrice, current, yoyGrowth, isRent);
            String unitLabel = isRent ? "万/㎡/月" : "万/㎡";
            String totalLabel = isRent ? "预测租金" : "预测总价";

            return String.format(
                    "%d年%d月【%s】本房源在%s%s为%.2f万，单位价格%.2f%s。数据库%s同月均价%.2f%s（样本%d条），较%d年同期%s%.1f%%。%s",
                    year, month, typeLabel, district, totalLabel, predictTotalPrice, predictUnitPrice, unitLabel,
                    typeLabel, current.getAvgUnitPrice(), unitLabel, current.getSampleCount(),
                    year - 1, yoyGrowth >= 0 ? "增长" : "下降", Math.abs(yoyGrowth), action);
        } catch (Exception e) {
            return "建议生成失败：" + e.getMessage();
        }
    }

    private MarketStats fetchStatsWithFallback(String district, int year, int month, double area, String listingType) throws Exception {
        MarketStats stats = dao.getMarketStats(district, year, month, area, listingType);
        if (stats.getSampleCount() >= MIN_SAMPLES) return stats;
        return dao.getMarketStats(district, year, month, 0, listingType);
    }

    private String baseLine(int year, int month, String district, String typeLabel, boolean isRent, double total, double unit) {
        if (isRent) {
            return String.format("%d年%d月【%s】本房源在%s预测租金%.2f万，单位月租%.2f万/㎡/月。", year, month, typeLabel, district, total, unit);
        }
        return String.format("%d年%d月【%s】本房源在%s预测总价%.2f万，单位价格%.2f万/㎡。", year, month, typeLabel, district, total, unit);
    }

    private String decideAction(double predictUnit, MarketStats current, double yoyGrowth, boolean isRent) {
        double market = current.getAvgUnitPrice();
        double diffPct = (predictUnit - market) / market * 100.0;

        if (isRent) {
            if (diffPct > 8.0) return yoyGrowth > 3.0 ? "预测月租高于市场均值且租金同比上涨，建议适度下调租金或延长招租周期。" : "预测月租高于市场均值，建议下调租金提升出租竞争力。";
            if (diffPct < -8.0) return "预测月租低于市场均值，出租性价比较高，建议尽快出租。";
            if (yoyGrowth > 5.0) return "预测月租处于合理区间，区域租金同比上涨，可按计划挂牌出租。";
            if (yoyGrowth < -3.0) return "预测月租处于合理区间，但区域租金走弱，建议灵活议价。";
            return "预测月租处于合理区间，建议结合楼层与地铁距离综合定价。";
        }

        if (diffPct > 8.0) return yoyGrowth > 3.0 ? "预测价高于市场均价且区域房价上涨，建议适当降价或观望后挂牌。" : "预测价明显高于市场均价，建议下调预期或延长挂牌周期。";
        if (diffPct < -8.0) return yoyGrowth < -2.0 ? "预测价低于市场均价且区域同比下行，建议尽快挂牌争取成交。" : "预测价低于市场均价，性价比较高，建议尽快挂牌。";
        if (yoyGrowth > 5.0) return "预测价处于市场合理区间，区域同比上涨，建议按计划挂牌。";
        if (yoyGrowth < -3.0) return "预测价处于市场合理区间，但区域同比走弱，建议灵活议价。";
        return "预测价处于市场合理区间，建议结合房源条件综合决策。";
    }
}