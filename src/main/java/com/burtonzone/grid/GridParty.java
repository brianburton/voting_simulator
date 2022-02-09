package com.burtonzone.grid;

import com.burtonzone.election.Party;
import java.util.Comparator;
import lombok.Value;

@Value
public class GridParty
{
    GridPosition position;
    Party party;

    public GridParty(GridPosition position,
                     int distance)
    {
        this.position = position;
        party = new Party(String.format("%s-%d", position, distance),
                          position.toString(),
                          position);
    }

    public static Comparator<GridParty> distanceComparator(GridPosition position)
    {
        return new DistanceComparator<>(position, GridParty::getPosition);
    }
}
