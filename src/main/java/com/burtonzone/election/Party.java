package com.burtonzone.election;

import com.burtonzone.common.Decimal;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.javimmutable.collections.JImmutableList;

@Data
@AllArgsConstructor
public class Party
{
    public static final Comparator<Party> NameComparator = Comparator.comparing(Party::getName);

    private final String name;
    private final String abbrev;
    private final Position position;

    @Override
    public String toString()
    {
        return name;
    }

    public static Decimal maxInterPartyDistance(JImmutableList<Party> parties)
    {
        Decimal max = Decimal.ZERO;
        for (int i = 0; i < parties.size() - 1; ++i) {
            var pi = parties.get(i);
            for (int j = i + 1; j < parties.size(); ++j) {
                var pj = parties.get(j);
                var distance = pi.getPosition().distanceTo(pj.getPosition());
                if (distance.isGreaterThan(max)) {
                    max = distance;
                }
            }
        }
        return max;
    }

    public static Comparator<Party> distanceComparator(Position position)
    {
        return new DistanceComparator<>(position, Party::getPosition);
    }
}
