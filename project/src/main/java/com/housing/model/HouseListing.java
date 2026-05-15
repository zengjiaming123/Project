package com.housing.model;

public class HouseListing {
    private String district;
    private int year;
    private int month;
    private double price;
    private double area;
    private int floor;
    private double houseAge;
    private double distanceToSubway;
    private String listingType;

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public double getHouseAge() { return houseAge; }
    public void setHouseAge(double houseAge) { this.houseAge = houseAge; }
    public double getDistanceToSubway() { return distanceToSubway; }
    public void setDistanceToSubway(double distanceToSubway) { this.distanceToSubway = distanceToSubway; }
    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }
}