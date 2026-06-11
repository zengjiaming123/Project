package com.housing.service;

import com.housing.model.LinearRegressionModel;
import com.housing.util.ListingTypeUtil;

import java.util.List;
import java.util.Map;

public class PredictionService {

    private final RegressionService regressionService = RegressionService.getInstance();

    public double predictUnitPrice(String district, int year, int month, double area, int floor,
                                   double distance, double age, String listingType) throws Exception {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) throw new IllegalArgumentException("请先在首页选择租房或购房");
        if (area <= 0) throw new IllegalArgumentException("面积必须大于 0");

        double totalPrice = regressionService.predictTotalPrice(
                district, year, month, area, floor, distance, age, type);
        return totalPrice / area;
    }

    public Map<String, Double> factorImpact(String district, int year, int month, double area, int floor,
                                            double distance, double age, String listingType) throws Exception {
        String type = ListingTypeUtil.normalize(listingType);
        if (type == null) throw new IllegalArgumentException("请先在首页选择租房或购房");
        return regressionService.factorImpact(district, year, month, area, floor, distance, age, type);
    }

    public LinearRegressionModel getModel(String listingType) throws Exception {
        return regressionService.getModel(listingType);
    }

    public Map<String, List<double[]>> getFitLines(String district, int year, int month, double area, int floor,
                                                   double distance, double age, String listingType) throws Exception {
        return regressionService.buildFitLines(district, year, month, area, floor, distance, age, listingType);
    }
}
