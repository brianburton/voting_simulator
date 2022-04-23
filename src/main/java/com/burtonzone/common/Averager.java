package com.burtonzone.common;

import static com.burtonzone.common.Decimal.ONE;
import static com.burtonzone.common.Decimal.ZERO;

public class Averager
{
    private Decimal sum = ZERO;
    private Decimal count = ZERO;

    public void add(Decimal value)
    {
        sum = sum.plus(value.square());
        count = count.plus(ONE);
    }

    public void add(Decimal value,
                    Decimal weight)
    {
        sum = sum.plus(value.square().times(weight));
        count = count.plus(weight);
    }

    public void add(int value)
    {
        add(new Decimal(value));
    }

    public Decimal average()
    {
        return sum.divide(count).root();
    }
}
