package com.housing.service;

import com.housing.dao.HouseListingDAO;
import com.housing.model.HouseListing;
import com.housing.model.LinearRegressionModel;
import com.housing.util.ListingTypeUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从数据库全量训练 rent / sale 两套线性回归模型，并缓存系数。
 */
public class RegressionService {

    private static final RegressionService INSTANCE = new RegressionService();

    public static RegressionService getInstance() {
        return INSTANCE;
    }

    public static final String[] DISTRICTS = {
            "广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门", "肇庆"
    };

    private static final String INTERCEPT = "截距";
    private static final String[] NUMERIC_FEATURES = {
            "年份", "月份", "面积", "楼层", "房龄", "距地铁"
    };

    private final HouseListingDAO dao = new HouseListingDAO();
    private LinearRegressionModel rentModel;
    private LinearRegressionModel saleModel;
    private volatile boolean initialized;

    private RegressionService() {}

    public synchronized void ensureTrained() throws Exception {
        if (initialized) return;
        List<HouseListing> rentData = dao.fetchTrainingData(ListingTypeUtil.RENT);
        List<HouseListing> saleData = dao.fetchTrainingData(ListingTypeUtil.SALE);
        if (rentData.size() < 20) throw new IllegalStateException("租房训练样本不足");
        if (saleData.size() < 20) throw new IllegalStateException("购房训练样本不足");

        rentModel = buildModel(rentData);
        saleModel = buildModel(saleData);
        initialized = true;
    }

    public LinearRegressionModel getModel(String listingType) throws Exception {
        ensureTrained();
        return ListingTypeUtil.isRent(listingType) ? rentModel : saleModel;
    }

    public double predictTotalPrice(String district, int year, int month, double area, int floor,
                                    double distance, double age, String listingType) throws Exception {
        LinearRegressionModel model = getModel(listingType);
        double[] features = buildFeatureVector(district, year, month, area, floor, distance, age);
        double price = model.predict(features);
        return Math.max(price, 0.01);
    }

    /**
     * 固定其余特征为用户输入，仅变化单一特征，生成多元回归模型的偏效应拟合线。
     */
    public Map<String, List<double[]>> buildFitLines(String district, int year, int month, double area, int floor,
                                                     double distance, double age, String listingType) throws Exception {
        LinearRegressionModel model = getModel(listingType);
        Map<String, List<double[]>> lines = new LinkedHashMap<>();
        lines.put("area", sampleLine(model, district, year, month, area, floor, distance, age, 28, 142, 40, "area"));
        lines.put("floor", sampleLine(model, district, year, month, area, floor, distance, age, 1, 30, 30, "floor"));
        lines.put("distance", sampleLine(model, district, year, month, area, floor, distance, age, 0.1, 5.5, 40, "distance"));
        lines.put("age", sampleLine(model, district, year, month, area, floor, distance, age, 0.5, 29, 40, "age"));
        return lines;
    }

    /**
     * 返回各特征的边际系数 β：每增加 1 个单位，预测总价（万）的变化量。
     * 地区以广州为基准，其它城市显示相对广州的价差系数。
     */
    public Map<String, Double> factorImpact(String district, int year, int month, double area, int floor,
                                            double distance, double age, String listingType) throws Exception {
        LinearRegressionModel model = getModel(listingType);
        double[] beta = model.getCoefficients();

        Map<String, Double> map = new LinkedHashMap<>();
        map.put("年份(每+1年)", beta[1]);
        map.put("月份(每+1月)", beta[2]);
        map.put("面积(每+1㎡)", beta[3]);
        map.put("楼层(每+1层)", beta[4]);
        map.put("房龄(每+1年)", beta[5]);
        map.put("距地铁(每+1km)", beta[6]);
        map.put("地区(相对广州)", districtMarginal(beta, district));
        return map;
    }

    private double districtMarginal(double[] beta, String district) {
        int districtIndex = indexOfDistrict(district);
        if (districtIndex <= 0) return 0;
        return beta[6 + districtIndex];
    }

    private List<double[]> sampleLine(LinearRegressionModel model, String district, int year, int month,
                                      double area, int floor, double distance, double age,
                                      double min, double max, int steps, String varyKey) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double x = min + (max - min) * i / steps;
            double a = area;
            int f = floor;
            double d = distance;
            double ag = age;
            switch (varyKey) {
                case "area": a = x; break;
                case "floor": f = (int) Math.round(x); break;
                case "distance": d = x; break;
                case "age": ag = x; break;
                default: break;
            }
            double[] features = buildFeatureVector(district, year, month, a, f, d, ag);
            double y = Math.max(model.predict(features), 0.01);
            points.add(new double[]{x, y});
        }
        return points;
    }

    private LinearRegressionModel buildModel(List<HouseListing> data) {
        int featureCount = 1 + NUMERIC_FEATURES.length + (DISTRICTS.length - 1);
        String[] names = buildFeatureNames();
        double[][] x = new double[data.size()][featureCount];
        double[] y = new double[data.size()];

        for (int i = 0; i < data.size(); i++) {
            HouseListing item = data.get(i);
            x[i] = buildFeatureVector(
                    item.getDistrict(), item.getYear(), item.getMonth(),
                    item.getArea(), item.getFloor(), item.getDistanceToSubway(), item.getHouseAge());
            y[i] = item.getPrice();
        }

        LinearRegressionModel model = new LinearRegressionModel();
        model.train(x, y, names);
        return model;
    }

    private String[] buildFeatureNames() {
        List<String> names = new ArrayList<>();
        names.add(INTERCEPT);
        for (String f : NUMERIC_FEATURES) names.add(f);
        for (int i = 1; i < DISTRICTS.length; i++) {
            names.add("地区_" + DISTRICTS[i]);
        }
        return names.toArray(new String[0]);
    }

    private double[] buildFeatureVector(String district, int year, int month, double area,
                                        int floor, double distance, double age) {
        int featureCount = 1 + NUMERIC_FEATURES.length + (DISTRICTS.length - 1);
        double[] v = new double[featureCount];
        v[0] = 1;
        v[1] = year;
        v[2] = month;
        v[3] = area;
        v[4] = floor;
        v[5] = age;
        v[6] = distance;

        int districtIndex = indexOfDistrict(district);
        if (districtIndex > 0) {
            v[6 + districtIndex] = 1;
        }
        return v;
    }

    private int indexOfDistrict(String district) {
        for (int i = 0; i < DISTRICTS.length; i++) {
            if (DISTRICTS[i].equals(district)) return i;
        }
        return 0;
    }
}
