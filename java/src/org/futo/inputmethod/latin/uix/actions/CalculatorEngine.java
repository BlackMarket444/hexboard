package org.futo.inputmethod.latin.uix.actions;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Hexboard modification: arithmetic for the calculator action.
 *
 * Understands + - × ÷ (also * and /), parentheses, decimals, a leading minus and %. Uses decimal
 * arithmetic, so 0.1 + 0.2 is exactly 0.3. A percentage added to or subtracted from a number is
 * taken as a share of that number (200 + 10% = 220), as on a pocket calculator.
 */
public final class CalculatorEngine {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Thrown for input that cannot be worked out. The message is safe to show to the user. */
    public static final class CalcException extends Exception {
        public final boolean incomplete;

        CalcException(final String message, final boolean incomplete) {
            super(message);
            this.incomplete = incomplete;
        }
    }

    private final String mText;
    private int mPos = 0;
    private boolean mPurePercent = false;

    private CalculatorEngine(final String text) {
        mText = text;
    }

    public static BigDecimal evaluate(final String expression) throws CalcException {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            final char c = expression.charAt(i);
            if (c == ' ' || c == ',') continue;
            sb.append(c == '×' ? '*' : c == '÷' ? '/' : c == '−' ? '-' : c);
        }
        if (sb.length() == 0) throw new CalcException("", true);

        final CalculatorEngine engine = new CalculatorEngine(sb.toString());
        final BigDecimal value = engine.parseSum();
        if (engine.mPos < engine.mText.length()) {
            throw new CalcException("Can't read that", false);
        }
        return value;
    }

    /** A result as text: at most 12 significant digits, no trailing zeros. */
    public static String format(final BigDecimal value) {
        BigDecimal v = value.round(new MathContext(12, RoundingMode.HALF_UP));
        if (v.signum() == 0) return "0";
        v = v.stripTrailingZeros();
        final int digitsBeforePoint = v.precision() - v.scale();
        if (digitsBeforePoint > 15 || digitsBeforePoint < -8) {
            return v.toString().replace("E+", "E");
        }
        return v.toPlainString();
    }

    private boolean atEnd() {
        return mPos >= mText.length();
    }

    private char peek() {
        return atEnd() ? '\0' : mText.charAt(mPos);
    }

    private BigDecimal parseSum() throws CalcException {
        BigDecimal left = parseProduct();
        while (peek() == '+' || peek() == '-') {
            final char op = mText.charAt(mPos++);
            BigDecimal right = parseProduct();
            if (mPurePercent) right = left.multiply(right, MC);
            left = op == '+' ? left.add(right, MC) : left.subtract(right, MC);
        }
        mPurePercent = false;
        return left;
    }

    private BigDecimal parseProduct() throws CalcException {
        BigDecimal left = parseUnary();
        boolean pure = mPurePercent;
        while (true) {
            final char c = peek();
            final boolean implicit = c == '(';
            if (c != '*' && c != '/' && !implicit) break;
            if (!implicit) mPos++;
            final BigDecimal right = parseUnary();
            pure = false;
            if (c == '/') {
                if (right.signum() == 0) throw new CalcException("Can't divide by zero", false);
                left = left.divide(right, MC);
            } else {
                left = left.multiply(right, MC);
            }
        }
        mPurePercent = pure;
        return left;
    }

    private BigDecimal parseUnary() throws CalcException {
        if (peek() == '-') {
            mPos++;
            return parseUnary().negate();
        }
        if (peek() == '+') {
            mPos++;
            return parseUnary();
        }
        BigDecimal value = parsePrimary();
        mPurePercent = false;
        while (peek() == '%') {
            mPos++;
            value = value.divide(HUNDRED, MC);
            mPurePercent = true;
        }
        return value;
    }

    private BigDecimal parsePrimary() throws CalcException {
        if (atEnd()) throw new CalcException("", true);
        if (peek() == '(') {
            mPos++;
            final BigDecimal value = parseSum();
            if (peek() == ')') mPos++;
            else if (!atEnd()) throw new CalcException("Can't read that", false);
            return value;
        }
        final int start = mPos;
        boolean seenPoint = false;
        while (!atEnd()) {
            final char c = mText.charAt(mPos);
            if (c >= '0' && c <= '9') {
                mPos++;
            } else if (c == '.' && !seenPoint) {
                seenPoint = true;
                mPos++;
            } else {
                break;
            }
        }
        final String number = mText.substring(start, mPos);
        if (number.isEmpty()) throw new CalcException("Can't read that", false);
        if (number.equals(".")) throw new CalcException("", true);
        try {
            return new BigDecimal(number.startsWith(".") ? "0" + number : number);
        } catch (final NumberFormatException e) {
            throw new CalcException("Can't read that", false);
        }
    }
}
