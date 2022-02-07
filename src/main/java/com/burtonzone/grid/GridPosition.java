package com.burtonzone.grid;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.election.PartyPosition;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
public class GridPosition
    implements Comparable<GridPosition>,
               PartyPosition
{
    int x;
    int y;

    public String toString()
    {
        return String.format("%d-%d", x, y);
    }

    public int quickDistanceTo(GridPosition other)
    {
        var sumX = x - other.x;
        var sumY = y - other.y;
        return sumX * sumX + sumY * sumY;
    }

    public static int toQuickDistance(int realDistance)
    {
        return realDistance * realDistance;
    }

    public int realDistance(GridPosition other)
    {
        return new Decimal(quickDistanceTo(other)).root().toInt();
    }

    @Override
    public Decimal distanceTo(PartyPosition other)
    {
        return new Decimal(quickDistanceTo((GridPosition)other)).root();
    }

    public GridPosition centeredNearBy(Rand rand,
                                       int maxOffset,
                                       int bias)
    {
        var x = this.x + rand.nextInt(-maxOffset, maxOffset, bias);
        var y = this.y + rand.nextInt(-maxOffset, maxOffset, bias);
        return new GridPosition(x, y);
    }

    public GridPosition nearBy(Rand rand,
                               int maxOffset)
    {
        var x = this.x + rand.nextInt(-maxOffset, maxOffset);
        var y = this.y + rand.nextInt(-maxOffset, maxOffset);
        return new GridPosition(x, y);
    }

    public GridPosition wrapped(int minValue,
                                int maxValue)
    {
        var newX = wrap(x, minValue, maxValue);
        var newY = wrap(y, minValue, maxValue);
        if (newX == x && newY == y) {
            return this;
        }
        // If we wrapped one position but not the other we shift the unwrapped one
        // to the other side to put us into the opposite corner from where we started.
        if (newX != x && newY == y) {
            newY = maxValue - (y - minValue);
        } else if (newX == x) {
            newX = maxValue - (x - minValue);
        }
        return new GridPosition(newX, newY);
    }

    @Override
    public int compareTo(GridPosition o)
    {
        var diff = x - o.x;
        if (diff == 0) {
            diff = y - o.y;
        }
        return diff;
    }

    private static int wrap(int value,
                            int minValue,
                            int maxValue)
    {
        if (value < minValue) {
            value = maxValue - (minValue - value);
        } else if (value > maxValue) {
            value = minValue + (value - maxValue);
        }
        assert value >= minValue;
        assert value <= maxValue;
        return value;
    }

    @AllArgsConstructor
    public static class DistanceComparator
        implements Comparator<GridPosition>
    {
        private final GridPosition center;

        @Override
        public int compare(GridPosition a,
                           GridPosition b)
        {
            var aDistance = center.quickDistanceTo(a);
            var bDistance = center.quickDistanceTo(b);
            var diff = aDistance - bDistance;
            if (diff == 0) {
                diff = a.compareTo(b);
            }
            return diff;
        }
    }
}
