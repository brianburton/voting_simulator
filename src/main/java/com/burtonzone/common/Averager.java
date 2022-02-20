package com.burtonzone.common;

import static com.burtonzone.common.Decimal.*;

public class Averager
{
    private Decimal sum = ZERO;
    private Decimal count = ZERO;

    public void add(Decimal value)
    {
        sum = sum.plus(value.squared());
        count = count.plus(ONE);
    }

    public void add(int value)
    {
        add(new Decimal(value));
    }

    public Decimal average()
    {
        return sum.dividedBy(count).root();
    }
}
