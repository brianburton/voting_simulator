package com.burtonzone.common;

import static com.burtonzone.common.Decimal.ZERO;

import java.util.Comparator;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.iterators.TransformIterator;
import org.javimmutable.collections.util.JImmutables;

public class Counter<T>
    implements IterableStreamable<Counter.Entry<T>>
{
    private final JImmutableMap<T, Decimal> counts;

    public Counter()
    {
        this(JImmutables.map());
    }

    public Counter(JImmutableMap<T, Decimal> counts)
    {
        this.counts = counts;
    }

    public boolean isEmpty()
    {
        return counts.isEmpty();
    }

    public Counter<T> addZero(T key)
    {
        if (counts.get(key) != null) {
            return this;
        } else {
            return new Counter<>(counts.assign(key, ZERO));
        }
    }

    public Counter<T> addZeros(Iterable<T> keys)
    {
        var answer = this;
        for (T key : keys) {
            answer = answer.addZero(key);
        }
        return answer;
    }

    public Counter<T> add(T key,
                          Decimal count)
    {
        if (count.isNegOrZero()) {
            return this;
        }
        var newCounts = counts.update(key, h -> h.orElse(ZERO).plus(count));
        return new Counter<>(newCounts);
    }

    public Counter<T> add(T key,
                          int count)
    {
        return add(key, new Decimal(count));
    }

    public Counter<T> add(Counter<T> other)
    {
        var answer = this;
        for (Entry<T> e : other) {
            answer = answer.add(e.getKey(), e.getCount());
        }
        return answer;
    }

    public Counter<T> subtract(T key,
                               Decimal count)
    {
        var current = get(key);
        if (count.isGreaterThan(current)) {
            throw new IllegalArgumentException("trying to subtract more than available");
        }
        var changed = current.minus(count);
        if (changed.isZero()) {
            return delete(key);
        } else {
            return new Counter<>(counts.assign(key, changed));
        }
    }


    public Counter<T> delete(T key)
    {
        var newCounts = counts.delete(key);
        return newCounts == counts ? this : new Counter<>(newCounts);
    }

    public Decimal get(T key)
    {
        return counts.getValueOr(key, ZERO);
    }

    public Decimal getTotal()
    {
        return counts.values().reduce(ZERO, Decimal::plus);
    }

    public int size()
    {
        return counts.size();
    }

    public JImmutableList<Entry<T>> getSortedList(Comparator<T> tieBreaker)
    {
        var comparator = highestFirstOrder(tieBreaker);
        return counts.stream()
            .map(Entry::new)
            .sorted(comparator)
            .collect(JImmutables.listCollector());
    }

    public Comparator<Entry<T>> highestFirstOrder(Comparator<T> tieBreaker)
    {
        return Comparator
            .<Entry<T>, Decimal>comparing(e -> e.count)
            .reversed()
            .thenComparing(e -> e.key, tieBreaker);
    }

    @Nonnull
    @Override
    public SplitableIterator<Entry<T>> iterator()
    {
        return TransformIterator.of(counts.iterator(), Entry::new);
    }

    @Override
    public int getSpliteratorCharacteristics()
    {
        return counts.getSpliteratorCharacteristics();
    }

    @Getter
    @AllArgsConstructor
    public static class Entry<T>
    {
        private final T key;
        private final Decimal count;

        private Entry(JImmutableMap.Entry<T, Decimal> e)
        {
            key = e.getKey();
            count = e.getValue();
        }
    }
}
