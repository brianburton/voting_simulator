package com.burtonzone.election;

import java.util.Comparator;
import java.util.function.Function;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DistanceComparator<T>
    implements Comparator<T>
{
    private final Position position;
    private final Function<T, Position> getter;

    @Override
    public int compare(T a,
                       T b)
    {
        var distA = position.squaredDistanceTo(getter.apply(a));
        var distB = position.squaredDistanceTo(getter.apply(b));
        return distA - distB;
    }
}
