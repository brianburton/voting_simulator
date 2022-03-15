package com.burtonzone.election;

import com.burtonzone.common.DataUtils;
import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;

@AllArgsConstructor
@EqualsAndHashCode
public class GridPosition
    implements Position
{
    public static final GridPosition Center = new GridPosition((MinPos + MaxPos) / 2, (MinPos + MaxPos) / 2);

    @Getter
    private final int x;
    @Getter
    private final int y;

    @Override
    public String toString()
    {
        return String.format("%d-%d", x, y);
    }

    @Override
    public int squaredDistanceTo(Position other)
    {
        final GridPosition otherPosition = (GridPosition)other;
        var diffX = x - otherPosition.x;
        var diffY = y - otherPosition.y;
        return diffX * diffX + diffY * diffY;
    }

    @Override
    public GridPosition nearBy(Rand rand,
                               int maxOffset,
                               int bias)
    {
        var x = this.x + rand.nextInt(-maxOffset, maxOffset, bias);
        var y = this.y + rand.nextInt(-maxOffset, maxOffset, bias);
        return new GridPosition(x, y);
    }

    @Override
    public Position moveDistance(Rand rand,
                                 int distance)
    {
        var xOffset = rand.nextInt(-distance, distance);
        var yOffsetSquared = distance * distance - xOffset * xOffset;
        var yOffset = new Decimal(yOffsetSquared).root().rounded().toInt();
        if (rand.nextBoolean()) {
            yOffset = -yOffset;
        }
        var x = this.x + xOffset;
        var y = rand.nextBoolean() ? this.y + yOffset : this.y - yOffset;
        return new GridPosition(x, y);
    }

    @Override
    public boolean isValid()
    {
        return x >= MinPos && y >= MinPos && x <= MaxPos && y <= MaxPos;
    }

    @Override
    public GridPosition wrapped(int minValue,
                                int maxValue)
    {
        var newX = DataUtils.wrap(x, minValue, maxValue);
        var newY = DataUtils.wrap(y, minValue, maxValue);
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
    public Position towards(Position other,
                            int divisor)
    {
        final GridPosition otherPosition = (GridPosition)other;
        var diffX = x - otherPosition.x;
        var diffY = y - otherPosition.y;
        return new GridPosition(x + diffX / divisor, y + diffY / divisor);
    }

    @Override
    public int compareTo(@Nonnull Position other)
    {
        final GridPosition otherPosition = (GridPosition)other;
        var diff = x - otherPosition.x;
        if (diff == 0) {
            diff = y - otherPosition.y;
        }
        return diff;
    }

    public static Position centerOf(JImmutableList<Position> positions)
    {
        var sumX = 0;
        var sumY = 0;
        for (Position position : positions) {
            sumX += ((GridPosition)position).x;
            sumY += ((GridPosition)position).y;
        }
        return new GridPosition(sumX / positions.size(), sumY / positions.size());
    }
}
