package com.housing.util;

/**
 * 统一租售类型：租房只用 rent，购房只用 sale
 */
public final class ListingTypeUtil {

    public static final String RENT = "rent";
    public static final String SALE = "sale";

    private ListingTypeUtil() {}

    public static String normalize(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) return null;
        if ("rent".equals(v) || "租房".equals(v)) return RENT;
        if ("sale".equals(v) || "buy".equals(v) || "购房".equals(v)) return SALE;
        return null;
    }

    public static boolean isRent(String type) {
        return RENT.equals(type);
    }

    public static boolean isSale(String type) {
        return SALE.equals(type);
    }

    public static String label(String type) {
        if (isRent(type)) return "租房";
        if (isSale(type)) return "购房";
        return "未知";
    }
}
