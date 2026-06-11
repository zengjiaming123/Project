package com.housing.model;

/**
 * 数据库中某一地区-年月-租售类型的市场切片（用于资讯栏随机抽样）
 */
public class MarketSnapshot {
    private String district;
    private int year;
    private int month;
    private String listingType;
    private int sampleCount;

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }
    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }
}
