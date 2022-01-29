package com.burtonzone.grid;

import com.burtonzone.election.Party;
import lombok.Value;

@Value
public class GridParty
{
    Position position;
    Party party;

    public GridParty(Position position)
    {
        this.position = position;
        party = new Party(position.toString(), String.format("%d-%d", position.getX() / 10, position.getY() / 10));
    }
}
