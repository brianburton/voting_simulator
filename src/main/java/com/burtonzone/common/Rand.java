package com.burtonzone.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Rand
{
    private final Random random;
    @Getter
    private final long seed;

    public Rand()
    {
        this(System.currentTimeMillis());
    }

    public Rand(long seed)
    {
        this(new Random(seed), seed);
    }

    public Rand(Random random,
                long seed)
    {
        this.random = random;
        this.seed = seed;
    }

    public int nextIndex(int collectionSize)
    {
        return random.nextInt(collectionSize);
    }

    public int nextIndex(int collectionSize,
                         int bias)
    {
        return nextInt(0, collectionSize - 1, bias);
    }

    public int nextInt(int min,
                       int max)
    {
        if (max <= min) {
            return min;
        } else {
            return min + random.nextInt(max - min + 1);
        }
    }

    public int nextInt(int min,
                       int max,
                       int bias)
    {
        if (max <= min || bias <= 0) {
            return min;
        } else {
            var sum = 0;
            for (int i = 1; i <= bias; ++i) {
                sum += random.nextInt(max - min + 1);
            }
            return min + sum / bias;
        }
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

    public <T> T nextElement(JImmutableList<T> list,
                             int bias)
    {
        var index = nextIndex(list.size(), bias);
        return list.get(index);
    }

    public boolean nextBoolean()
    {
        return random.nextBoolean();
    }
}
