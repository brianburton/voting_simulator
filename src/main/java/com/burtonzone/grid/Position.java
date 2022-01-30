package com.burtonzone.grid;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
public class Position
    implements Comparable<Position>
{
    int x;
    int y;

    public String toString()
    {
        return String.format("%d-%d", x, y);
    }

    public int quickDistance(Position other)
    {
        var sumX = x - other.x;
        var sumY = y - other.y;
        return sumX * sumX + sumY * sumY;
    }

    public int realDistance(Position other)
    {
        return new Decimal(quickDistance(other)).root().toInt();
    }

    public Position centeredNearBy(Rand rand,
                                   int maxOffset)
    {
        final var bias = 4;
        var x = this.x + rand.nextInt(-maxOffset, maxOffset, bias);
        var y = this.y + rand.nextInt(-maxOffset, maxOffset, bias);
        return new Position(x, y);
    }

    public Position nearBy(Rand rand,
                           int maxOffset)
    {
        var x = this.x + rand.nextInt(-maxOffset, maxOffset);
        var y = this.y + rand.nextInt(-maxOffset, maxOffset);
        return new Position(x, y);
    }

    public Position wrapped(int minValue,
                            int maxValue)
    {
        var newX = wrap(x, minValue, maxValue);
        var newY = wrap(y, minValue, maxValue);
        if (newX == x && newY == y) {
            return this;
        } else {
            return new Position(newX, newY);
        }
    }

    @Override
    public int compareTo(Position o)
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
        implements Comparator<Position>
    {
        private final Position center;

        @Override
        public int compare(Position a,
                           Position b)
        {
            var aDistance = center.quickDistance(a);
            var bDistance = center.quickDistance(b);
            var diff = aDistance - bDistance;
            if (diff == 0) {
                diff = a.compareTo(b);
            }
            return diff;
        }
    }
}