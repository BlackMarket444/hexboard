package org.futo.inputmethod.latin.uix.actions;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Hexboard modification: unit conversion tables for the converter action. Works fully offline.
 * US customary units are listed first and use US definitions (US gallon, short ton).
 */
public final class UnitConverter {
    private static final MathContext MC = MathContext.DECIMAL64;

    public static final class Category {
        public final String name;
        public final String[] units;
        /** How many base units one of each unit is; null for temperature, which is not a ratio. */
        private final BigDecimal[] factors;

        Category(final String name, final String[] units, final String[] factors) {
            this.name = name;
            this.units = units;
            if (factors == null) {
                this.factors = null;
            } else {
                this.factors = new BigDecimal[factors.length];
                for (int i = 0; i < factors.length; i++) {
                    final String f = factors[i];
                    final int slash = f.indexOf('/');
                    this.factors[i] = slash < 0 ? new BigDecimal(f)
                            : new BigDecimal(f.substring(0, slash)).divide(new BigDecimal(f.substring(slash + 1)), MC);
                }
            }
        }

        public BigDecimal convert(final BigDecimal value, final int from, final int to) {
            if (from == to) return value;
            if (factors != null) {
                return value.multiply(factors[from], MC).divide(factors[to], MC);
            }
            // Temperature: go through Celsius. Order is °F, °C, K.
            final BigDecimal c;
            if (from == 0) c = value.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5), MC).divide(BigDecimal.valueOf(9), MC);
            else if (from == 1) c = value;
            else c = value.subtract(new BigDecimal("273.15"));
            if (to == 0) return c.multiply(BigDecimal.valueOf(9), MC).divide(BigDecimal.valueOf(5), MC).add(BigDecimal.valueOf(32));
            if (to == 1) return c;
            return c.add(new BigDecimal("273.15"));
        }
    }

    public static final Category[] CATEGORIES = {
        new Category("Length",
                new String[]{"in", "ft", "yd", "mi", "mm", "cm", "m", "km"},
                new String[]{"0.0254", "0.3048", "0.9144", "1609.344", "0.001", "0.01", "1", "1000"}),
        new Category("Weight",
                new String[]{"oz", "lb", "ton", "g", "kg", "tonne"},
                new String[]{"0.028349523125", "0.45359237", "907.18474", "0.001", "1", "1000"}),
        new Category("Temp",
                new String[]{"°F", "°C", "K"},
                null),
        new Category("Volume",
                new String[]{"tsp", "tbsp", "fl oz", "cup", "pt", "qt", "gal", "mL", "L"},
                new String[]{"0.00492892159375", "0.01478676478125", "0.0295735295625", "0.2365882365",
                        "0.473176473", "0.946352946", "3.785411784", "0.001", "1"}),
        new Category("Area",
                new String[]{"sq ft", "sq yd", "acre", "sq mi", "m²", "ha", "km²"},
                new String[]{"0.09290304", "0.83612736", "4046.8564224", "2589988.110336", "1", "10000", "1000000"}),
        new Category("Speed",
                new String[]{"mph", "ft/s", "knot", "km/h", "m/s"},
                new String[]{"0.44704", "0.3048", "1852/3600", "1000/3600", "1"}),
        new Category("Time",
                new String[]{"sec", "min", "hr", "day", "week", "year"},
                new String[]{"1", "60", "3600", "86400", "604800", "31536000"}),
    };

    private UnitConverter() { }
}
