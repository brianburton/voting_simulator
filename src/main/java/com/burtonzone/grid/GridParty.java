package com.burtonzone.grid;

import com.burtonzone.election.Party;
import lombok.Value;

@Value
public class GridParty
{
    Position position;
    Party party;

    public GridParty(Position position,
                     int distance)
    {
        this.position = position;
        party = new Party(String.format("%s-%d", position, distance),
                          String.format("%d-%d", position.getX() / 10, position.getY() / 10));
    }
}
