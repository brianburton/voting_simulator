package com.burtonzone.common;

import static org.javimmutable.collections.util.JImmutables.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DataUtilsTest
{
    @Test
    public void testCombos()
    {
        var ints = list(1, 2, 3, 4, 5);
        assertEquals(list(list(1), list(2), list(3), list(4), list(5)),
                     DataUtils.combos(ints, 1));
        assertEquals(list(list(1, 2), list(1, 3), list(1, 4), list(1, 5),
                          list(2, 3), list(2, 4), list(2, 5),
                          list(3, 4), list(3, 5),
                          list(4, 5)),
                     DataUtils.combos(ints, 2));
        assertEquals(list(list(1, 2, 3), list(1, 2, 4), list(1, 2, 5),
                          list(1, 3, 4), list(1, 3, 5),
                          list(1, 4, 5),
                          list(2, 3, 4), list(2, 3, 5),
                          list(2, 4, 5),
                          list(3, 4, 5)),
                     DataUtils.combos(ints, 3));
        assertEquals(list(list(1, 2, 3, 4), list(1, 2, 3, 5), list(1, 2, 4, 5),
                          list(1, 3, 4, 5),
                          list(2, 3, 4, 5)),
                     DataUtils.combos(ints, 4));
    }
}
