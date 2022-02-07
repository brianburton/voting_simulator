package com.burtonzone.grid;

import static org.junit.Assert.*;

import org.junit.Test;

public class GridPositionTest
{
    @Test
    public void wrapX()
    {
        assertEquals(new GridPosition(95, 80), new GridPosition(-5, 20).wrapped(0, 100));
        assertEquals(new GridPosition(20, 20), new GridPosition(120, 80).wrapped(0, 100));
    }

    @Test
    public void wrapY()
    {
        assertEquals(new GridPosition(90, 80), new GridPosition(10, -20).wrapped(0, 100));
        assertEquals(new GridPosition(10, 10), new GridPosition(90, 110).wrapped(0, 100));
    }

    @Test
    public void wrapBoth()
    {
        assertEquals(new GridPosition(95, 80), new GridPosition(-5, -20).wrapped(0, 100));
        assertEquals(new GridPosition(95, 10), new GridPosition(-5, 110).wrapped(0, 100));
        assertEquals(new GridPosition(18, 65), new GridPosition(118, -35).wrapped(0, 100));
        assertEquals(new GridPosition(40, 15), new GridPosition(140, 115).wrapped(0, 100));
    }

    @Test
    public void noWrap()
    {
        var pos = new GridPosition(10, 90);
        assertSame(pos, pos.wrapped(0, 100));
    }
}
