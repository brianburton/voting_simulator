package com.burtonzone.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.stream.Collector;
import lombok.EqualsAndHashCode;
import org.javimmutable.collections.GenericCollector;

@EqualsAndHashCode
public class Decimal
    implements Comparable<Decimal>
{
    private static final int PRECISION = 8;
    private static final int DIVISION_PRECISION = 4 * PRECISION;
    private static final BigDecimal ZERO_BOUND = new BigDecimal(5).scaleByPowerOfTen(-PRECISION);
    public static final Decimal ZERO = new Decimal(0);
    public static final Decimal ONE = new Decimal(1);
    public static final Decimal TWO = new Decimal(2);
    public static final Decimal HUNDRED = new Decimal(100);

    private final BigDecimal value;

    public Decimal(int value)
    {
        this(new BigDecimal(value));
    }

    public Decimal(String value)
    {
        this(new BigDecimal(value));
    }

    public Decimal(BigDecimal value)
    {
        this.value = value.setScale(PRECISION, RoundingMode.HALF_UP);
    }

    public static Collector<Decimal, ?, Decimal> collectSum()
    {
        return GenericCollector.unordered(ZERO,
                                          ZERO,
                                          Decimal::isZero,
                                          Decimal::plus,
                                          Decimal::plus);
    }

    public Decimal plus(Decimal other)
    {
        return new Decimal(value.add(other.value));
    }

    public Decimal plus(int val)
    {
        return new Decimal(value.add(new BigDecimal(val)));
    }

    public Decimal minus(Decimal other)
    {
        return new Decimal(value.subtract(other.value));
    }

    public Decimal times(Decimal other)
    {
        return new Decimal(value.multiply(other.value, MathContext.UNLIMITED));
    }

    public Decimal divide(Decimal divisor)
    {
        return new Decimal(value.divide(divisor.value, DIVISION_PRECISION, RoundingMode.HALF_UP));
    }

    public Decimal divide(int divisor)
    {
        return divide(new Decimal(divisor));
    }

    public Decimal div(Decimal o)
    {
        return divide(o).roundDown();
    }

    public Decimal mod(Decimal o)
    {
        return minus(div(o).times(o));
    }

    public Decimal abs()
    {
        return value.signum() >= 0 ? this : new Decimal(value.negate());
    }

    public Decimal square()
    {
        return times(this);
    }

    public Decimal root()
    {
        return new Decimal(value.sqrt(new MathContext(DIVISION_PRECISION, RoundingMode.HALF_UP)));
    }

    public Decimal round()
    {
        return new Decimal(value.setScale(0, RoundingMode.HALF_UP));
    }

    public Decimal roundUp()
    {
        return new Decimal(value.setScale(0, RoundingMode.UP));
    }

    public Decimal roundDown()
    {
        return new Decimal(value.setScale(0, RoundingMode.DOWN));
    }

    public boolean isGreaterThan(Decimal other)
    {
        return compareTo(other) > 0;
    }

    public boolean isGreaterOrEqualTo(Decimal other)
    {
        return compareTo(other) >= 0;
    }

    public boolean isLessThan(Decimal other)
    {
        return compareTo(other) < 0;
    }

    public boolean isLessOrEqualTo(Decimal other)
    {
        return compareTo(other) <= 0;
    }

    public boolean isZero()
    {
        var diff = minus(ZERO).abs();
        return diff.value.compareTo(ZERO_BOUND) <= 0;
    }

    public boolean isNegOrZero()
    {
        return value.signum() <= 0 || isZero();
    }

    public BigDecimal toBigDecimal()
    {
        return value;
    }

    public int toInt()
    {
        return value.toBigInteger().intValue();
    }

    @Override
    public int compareTo(Decimal other)
    {
        return value.compareTo(other.value);
    }

    @Override
    public String toString()
    {
        return value.toPlainString();
    }
}
