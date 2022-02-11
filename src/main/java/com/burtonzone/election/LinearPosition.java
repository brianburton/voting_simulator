package com.burtonzone.election;

import static com.burtonzone.common.DataUtils.wrap;

import com.burtonzone.common.Rand;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
public class LinearPosition
    implements PartyPosition
{
    public static LinearPosition Center = new LinearPosition((MinPos + MaxPos) / 2);

    private final int x;

    @Override
    public String toString()
    {
        return String.format("%02d", x);
    }

    @Override
    public int squaredDistanceTo(PartyPosition other)
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
    public int compareTo(@Nonnull PartyPosition other)
    {
        var otherPosition = (LinearPosition)other;
        return x - otherPosition.x;
    }
}
