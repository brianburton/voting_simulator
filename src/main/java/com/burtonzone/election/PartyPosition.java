package com.burtonzone.election;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import java.util.Comparator;
import lombok.AllArgsConstructor;

public interface PartyPosition
    extends Comparable<PartyPosition>
{
    int MaxPos = 100;
    int MinPos = 0;

    int squaredDistanceTo(PartyPosition other);

    default Decimal distanceTo(PartyPosition other)
    {
        return new Decimal(squaredDistanceTo(other)).root();
    }

    PartyPosition nearBy(Rand rand,
                         int maxOffset,
                         int bias);

    PartyPosition wrapped(int minValue,
                          int maxValue);

    static int toSquaredDistance(int realDistance)
    {
        return realDistance * realDistance;
    }

    @AllArgsConstructor
    class DistanceComparator
        implements Comparator<PartyPosition>
    {
        private final PartyPosition center;

        @Override
        public int compare(PartyPosition a,
                           PartyPosition b)
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
