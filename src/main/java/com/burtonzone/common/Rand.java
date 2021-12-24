package com.burtonzone.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Rand
{
    private final Random random;

    public Rand()
    {
        this(System.currentTimeMillis());
    }

    public Rand(long seed)
    {
        this(new Random(seed));
    }

    public Rand(Random random)
    {
        this.random = random;
    }

    public int nextIndex(int collectionSize)
    {
        var limit = 3 * collectionSize;
        var r1 = 1 + random.nextInt(limit);
        var r2 = 1 + random.nextInt(limit);
        var r3 = 1 + random.nextInt(limit);
        var avg = (r1 + r2 + r3) / 3;
        return (avg - 1) / 3;
    }

    public <T> JImmutableList<T> shuffle(Collection<T> collection)
    {
        var list = new ArrayList<T>(collection);
        Collections.shuffle(list, random);
        return JImmutables.list(list);
    }

    public <T> T nextElement(JImmutableList<T> list)
    {
        var index = nextIndex(list.size());
        return list.get(index);
    }
}
