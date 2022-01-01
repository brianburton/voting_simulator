package com.burtonzone.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class DecimalTest
{
    @Test
    public void zeroTest()
    {
        assertEquals("0.00000000", Decimal.ZERO.toString());

        assertTrue(new Decimal("0.00000000").isZero());
        assertTrue(new Decimal("0.00000001").isZero());
        assertTrue(new Decimal("0.00000002").isZero());
        assertTrue(new Decimal("0.00000003").isZero());
        assertTrue(new Decimal("0.00000004").isZero());
        assertTrue(new Decimal("0.00000005").isZero());

        assertTrue(new Decimal("-0.00000001").isZero());
        assertTrue(new Decimal("-0.00000002").isZero());
        assertTrue(new Decimal("-0.00000003").isZero());
        assertTrue(new Decimal("-0.00000004").isZero());
        assertTrue(new Decimal("-0.00000005").isZero());

        assertFalse(new Decimal("0.00000006").isZero());
        assertFalse(new Decimal("-0.00000006").isZero());
    }

    @Test
    public void negOrZeroTest()
    {
        assertTrue(new Decimal("-0.1").isNegOrZero());
        assertFalse(new Decimal("0.1").isNegOrZero());
        assertTrue(Decimal.ZERO.isNegOrZero());
        assertTrue(new Decimal("-0.00000001").isNegOrZero());
        assertTrue(new Decimal("0.00000001").isNegOrZero());
    }
}
