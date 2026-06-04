package com.housing.model;

/**
 * 某一查询条件下的市场统计（基于 house_listings 真实数据）
 */
public class MarketStats {
    private double avgUnitPrice;   // 万/㎡
    private double avgTotalPrice;  // 万
    private int sampleCount;

    public double getAvgUnitPrice() { return avgUnitPrice; }
    public void setAvgUnitPrice(double avgUnitPrice) { this.avgUnitPrice = avgUnitPrice; }
    public double getAvgTotalPrice() { return avgTotalPrice; }
    public void setAvgTotalPrice(double avgTotalPrice) { this.avgTotalPrice = avgTotalPrice; }
    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

    public boolean hasData() {
        return sampleCount > 0 && avgUnitPrice > 0;
    }
}
