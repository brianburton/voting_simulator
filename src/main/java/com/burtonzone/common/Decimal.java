package com.burtonzone.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public class Decimal
    implements Comparable<Decimal>
{
    public static final Decimal ZERO = new Decimal(0);
    public static final Decimal ONE = new Decimal(1);

    private final BigDecimal value;

    public Decimal(int value)
    {
        this(new BigDecimal(value));
    }

    private Decimal(BigDecimal value)
    {
        this.value = value.setScale(10, RoundingMode.HALF_UP);
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

    public Decimal squared()
    {
        return new Decimal(value.multiply(value));
    }

    public Decimal root()
    {
        return new Decimal(value.sqrt(MathContext.UNLIMITED));
    }

    public Decimal dividedBy(Decimal other)
    {
        return new Decimal(value.divide(other.value, MathContext.UNLIMITED));
    }

    public Decimal times(Decimal other)
    {
        return new Decimal(value.multiply(other.value, MathContext.UNLIMITED));
    }

    public Decimal times(int val)
    {
        return new Decimal(value.multiply(new BigDecimal(val), MathContext.UNLIMITED));
    }

    @Override
    public int compareTo(Decimal other)
    {
        return value.compareTo(other.value);
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
}
