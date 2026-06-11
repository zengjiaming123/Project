package com.housing.model;

/**
 * 多元线性回归：y = β0 + β1*x1 + ... + βn*xn，OLS 最小二乘拟合。
 */
public class LinearRegressionModel {

    private double[] coefficients;
    private String[] featureNames;
    private double rSquared;
    private int sampleCount;

    public void train(double[][] x, double[] y, String[] featureNames) {
        if (x == null || y == null || x.length == 0 || x.length != y.length) {
            throw new IllegalArgumentException("训练数据无效");
        }
        int n = x.length;
        int p = x[0].length;
        if (p < 2) throw new IllegalArgumentException("特征数不足");

        double[][] xtx = new double[p][p];
        double[] xty = new double[p];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p; j++) {
                xty[j] += x[i][j] * y[i];
                for (int k = 0; k < p; k++) {
                    xtx[j][k] += x[i][j] * x[i][k];
                }
            }
        }

        this.coefficients = solveSymmetric(xtx, xty);
        this.featureNames = featureNames.clone();
        this.sampleCount = n;
        this.rSquared = computeRSquared(x, y, coefficients);
    }

    public double predict(double[] features) {
        if (coefficients == null) throw new IllegalStateException("模型尚未训练");
        if (features.length != coefficients.length) {
            throw new IllegalArgumentException("特征维度与模型不匹配");
        }
        double sum = 0;
        for (int i = 0; i < coefficients.length; i++) {
            sum += coefficients[i] * features[i];
        }
        return sum;
    }

    public double contribution(int index, double featureValue) {
        if (coefficients == null || index < 0 || index >= coefficients.length) return 0;
        return coefficients[index] * featureValue;
    }

    public double getRSquared() { return rSquared; }
    public int getSampleCount() { return sampleCount; }
    public double[] getCoefficients() { return coefficients.clone(); }
    public String[] getFeatureNames() { return featureNames.clone(); }
    public boolean isTrained() { return coefficients != null; }

    private double computeRSquared(double[][] x, double[] y, double[] beta) {
        double mean = 0;
        for (double v : y) mean += v;
        mean /= y.length;

        double ssTot = 0;
        double ssRes = 0;
        for (int i = 0; i < y.length; i++) {
            double pred = 0;
            for (int j = 0; j < beta.length; j++) pred += beta[j] * x[i][j];
            ssTot += (y[i] - mean) * (y[i] - mean);
            ssRes += (y[i] - pred) * (y[i] - pred);
        }
        return ssTot == 0 ? 0 : 1 - ssRes / ssTot;
    }

    /** 高斯消元求解对称正定方程组 Ax = b */
    private double[] solveSymmetric(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }

        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) pivot = row;
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;

            double div = m[col][col];
            if (Math.abs(div) < 1e-12) div = 1e-12;
            for (int j = col; j <= n; j++) m[col][j] /= div;

            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = m[row][col];
                for (int j = col; j <= n; j++) {
                    m[row][j] -= factor * m[col][j];
                }
            }
        }

        double[] result = new double[n];
        for (int i = 0; i < n; i++) result[i] = m[i][n];
        return result;
    }
}
