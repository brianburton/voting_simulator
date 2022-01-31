package com.burtonzone.grid;

import static org.junit.Assert.*;

import org.junit.Test;

public class PositionTest
{
    @Test
    public void wrapX()
    {
        assertEquals(new Position(95, 80), new Position(-5, 20).wrapped(0, 100));
        assertEquals(new Position(20, 20), new Position(120, 80).wrapped(0, 100));
    }

    @Test
    public void wrapY()
    {
        assertEquals(new Position(90, 80), new Position(10, -20).wrapped(0, 100));
        assertEquals(new Position(10, 10), new Position(90, 110).wrapped(0, 100));
    }

    @Test
    public void wrapBoth()
    {
        assertEquals(new Position(95, 80), new Position(-5, -20).wrapped(0, 100));
        assertEquals(new Position(95, 10), new Position(-5, 110).wrapped(0, 100));
        assertEquals(new Position(18, 65), new Position(118, -35).wrapped(0, 100));
        assertEquals(new Position(40, 15), new Position(140, 115).wrapped(0, 100));
    }

    @Test
    public void noWrap()
    {
        var pos = new Position(10, 90);
        assertSame(pos, pos.wrapped(0, 100));
    }
}
