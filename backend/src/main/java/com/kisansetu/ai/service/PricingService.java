package com.kisansetu.ai.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static pricing intelligence (ported from the original project).
 * Prices come from a local lookup table — never from the AI — and are
 * clearly identified as estimates for planning purposes.
 */
@Service
public class PricingService {

    private static final Map<String, String[]> PRICE_TABLE = new LinkedHashMap<>();

    static {
        // Fungicides
        PRICE_TABLE.put("chlorothalonil", new String[]{"liquid", "900", null});
        PRICE_TABLE.put("mancozeb", new String[]{"powder", null, "350"});
        PRICE_TABLE.put("copper oxychloride", new String[]{"powder", null, "420"});
        PRICE_TABLE.put("carbendazim", new String[]{"powder", null, "480"});
        PRICE_TABLE.put("propiconazole", new String[]{"liquid", "1200", null});
        PRICE_TABLE.put("hexaconazole", new String[]{"liquid", "1100", null});
        PRICE_TABLE.put("metalaxyl", new String[]{"powder", null, "600"});
        PRICE_TABLE.put("thiram", new String[]{"powder", null, "380"});
        PRICE_TABLE.put("zineb", new String[]{"powder", null, "300"});
        PRICE_TABLE.put("sulfur", new String[]{"powder", null, "150"});
        PRICE_TABLE.put("bordeaux mixture", new String[]{"powder", null, "250"});
        PRICE_TABLE.put("trichoderma", new String[]{"powder", null, "400"});
        // Insecticides
        PRICE_TABLE.put("imidacloprid", new String[]{"liquid", "1800", null});
        PRICE_TABLE.put("chlorpyrifos", new String[]{"liquid", "650", null});
        PRICE_TABLE.put("cypermethrin", new String[]{"liquid", "750", null});
        PRICE_TABLE.put("dimethoate", new String[]{"liquid", "500", null});
        PRICE_TABLE.put("neem oil", new String[]{"liquid", "400", null});
        PRICE_TABLE.put("spinosad", new String[]{"liquid", "2200", null});
        PRICE_TABLE.put("fipronil", new String[]{"liquid", "1500", null});
        // Fertilizers
        PRICE_TABLE.put("urea", new String[]{"powder", null, "8"});
        PRICE_TABLE.put("dap", new String[]{"powder", null, "27"});
        PRICE_TABLE.put("potash", new String[]{"powder", null, "18"});
        PRICE_TABLE.put("ammonium nitrate", new String[]{"powder", null, "12"});
        PRICE_TABLE.put("organic compost", new String[]{"powder", null, "3"});
        PRICE_TABLE.put("micronutrient mix", new String[]{"powder", null, "200"});
        PRICE_TABLE.put("gypsum", new String[]{"powder", null, "12"});
        PRICE_TABLE.put("single super phosphate", new String[]{"powder", null, "14"});
        // Herbicides
        PRICE_TABLE.put("glyphosate", new String[]{"liquid", "600", null});
        PRICE_TABLE.put("2,4-d", new String[]{"liquid", "450", null});
        PRICE_TABLE.put("atrazine", new String[]{"powder", null, "350"});
        PRICE_TABLE.put("pendimethalin", new String[]{"liquid", "700", null});
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("([\\d.]+)");

    private record PriceEntry(String type, Double pricePerLiter, Double pricePerKg) {
    }

    private PriceEntry lookup(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lower = name.toLowerCase().trim();
        for (Map.Entry<String, String[]> entry : PRICE_TABLE.entrySet()) {
            if (entry.getKey().equals(lower)) {
                return toEntry(entry.getValue());
            }
        }
        for (Map.Entry<String, String[]> entry : PRICE_TABLE.entrySet()) {
            if (lower.contains(entry.getKey()) || entry.getKey().contains(lower)) {
                return toEntry(entry.getValue());
            }
        }
        return null;
    }

    private PriceEntry toEntry(String[] v) {
        Double perLiter = v[1] == null ? null : Double.parseDouble(v[1]);
        Double perKg = v[2] == null ? null : Double.parseDouble(v[2]);
        return new PriceEntry(v[0], perLiter, perKg);
    }

    /**
     * Computes unit price + total cost for a treatment based on dosage.
     * Returns null when the product is not in the price table.
     */
    public TreatmentCost calculateCost(String productName, String dosageStr) {
        PriceEntry entry = lookup(productName);
        if (entry == null || dosageStr == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(dosageStr);
        if (!matcher.find()) {
            return null;
        }
        double value;
        try {
            value = Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (value <= 0) {
            return null;
        }
        String dosageLower = dosageStr.toLowerCase();
        double unitPrice;
        double totalCost;
        String displayQty;

        if ("liquid".equals(entry.type) && entry.pricePerLiter != null) {
            unitPrice = entry.pricePerLiter;
            if (dosageLower.contains("ml")) {
                totalCost = (entry.pricePerLiter / 1000) * value;
                displayQty = value + " ml";
            } else if (dosageLower.contains("liter") || dosageLower.contains("litre") || dosageLower.contains("l")) {
                totalCost = entry.pricePerLiter * value;
                displayQty = value + " L";
            } else if (value >= 10) {
                totalCost = (entry.pricePerLiter / 1000) * value;
                displayQty = value + " ml";
            } else {
                totalCost = entry.pricePerLiter * value;
                displayQty = value + " L";
            }
        } else if ("powder".equals(entry.type) && entry.pricePerKg != null) {
            unitPrice = entry.pricePerKg;
            if (dosageLower.contains("gram") || dosageLower.contains(" g")) {
                totalCost = (entry.pricePerKg / 1000) * value;
                displayQty = value + " g";
            } else {
                totalCost = entry.pricePerKg * value;
                displayQty = value + " kg";
            }
        } else {
            return null;
        }

        return new TreatmentCost(Math.round(totalCost * 100) / 100.0, unitPrice, displayQty);
    }

    public record TreatmentCost(double totalCost, double unitPrice, String requiredQuantity) {
    }
}