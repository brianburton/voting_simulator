package com.burtonzone.election;

import static com.burtonzone.common.DataUtils.wrap;

import com.burtonzone.common.Rand;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.javimmutable.collections.JImmutableList;

@AllArgsConstructor
@EqualsAndHashCode
public class LinearPosition
    implements Position
{
    public static LinearPosition Center = new LinearPosition((MinPos + MaxPos) / 2);

    private final int x;

    @Override
    public String toString()
    {
        return String.format("%02d", x);
    }

    @Override
    public int squaredDistanceTo(Position other)
    {
        var otherPosition = (LinearPosition)other;
        var diff = x - otherPosition.x;
        return diff * diff;
    }

    @Override
    public LinearPosition nearBy(Rand rand,
                                 int maxOffset,
                                 int bias)
    {
        var position = this.x + rand.nextInt(-maxOffset, maxOffset, bias);
        return new LinearPosition(position);
    }

    @Override
    public Position moveDistance(Rand rand,
                                 int distance)
    {
        var offset = rand.nextBoolean() ? -distance : distance;
        return new LinearPosition(x + offset);
    }

    @Override
    public boolean isValid()
    {
        return x >= MinPos && x <= MaxPos;
    }

    @Override
    public LinearPosition wrapped(int minValue,
                                  int maxValue)
    {
        var newX = wrap(x, minValue, maxValue);
        if (newX == x) {
            return this;
        } else {
            return new LinearPosition(newX);
        }
    }

    @Override
    public Position towards(Position other,
                            int divisor)
    {
        var otherPosition = (LinearPosition)other;
        var diff = x - otherPosition.x;
        return new LinearPosition(x + diff / divisor);
    }

    @Override
    public int compareTo(@Nonnull Position other)
    {
        var otherPosition = (LinearPosition)other;
        return x - otherPosition.x;
    }

    public static Position centerOf(JImmutableList<Position> positions)
    {
        var sumX = 0;
        for (Position position : positions) {
            sumX += ((LinearPosition)position).x;
        }
        return new LinearPosition(sumX / positions.size());
    }
}
