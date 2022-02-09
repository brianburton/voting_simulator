package com.burtonzone.grid;

import java.util.Comparator;
import java.util.function.Function;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DistanceComparator<T>
    implements Comparator<T>
{
    private final GridPosition position;
    private final Function<T, GridPosition> getter;

    @Override
    public int compare(T a,
                       T b)
    {
        var distA = position.quickDistanceTo(getter.apply(a));
        var distB = position.quickDistanceTo(getter.apply(b));
        return distA - distB;
    }
}
