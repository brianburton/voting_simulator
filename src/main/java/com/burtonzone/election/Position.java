package com.burtonzone.election;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import java.util.Comparator;
import lombok.AllArgsConstructor;

public interface Position
    extends Comparable<Position>
{
    int MaxPos = 100;
    int MidPos = 50;
    int MinPos = 0;

    int squaredDistanceTo(Position other);

    default Decimal distanceTo(Position other)
    {
        return new Decimal(squaredDistanceTo(other)).root();
    }

    Position nearBy(Rand rand,
                    int maxOffset,
                    int bias);

    Position wrapped(int minValue,
                     int maxValue);

    Position towards(Position other,
                     int divisor);

    Position moveDistance(Rand rand,
                          int distance);

    boolean isValid();

    static int toSquaredDistance(int realDistance)
    {
        return realDistance * realDistance;
    }

    @AllArgsConstructor
    class DistanceComparator
        implements Comparator<Position>
    {
        private final Position center;

        @Override
        public int compare(Position a,
                           Position b)
        {
            var aDistance = center.squaredDistanceTo(a);
            var bDistance = center.squaredDistanceTo(b);
            var diff = aDistance - bDistance;
            if (diff == 0) {
                diff = a.compareTo(b);
            }
            return diff;
        }
    }
}
