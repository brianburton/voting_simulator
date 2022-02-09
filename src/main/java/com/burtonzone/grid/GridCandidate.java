package com.burtonzone.grid;

import com.burtonzone.election.Candidate;
import java.util.Comparator;
import lombok.Value;

@Value
public class GridCandidate
{
    GridParty party;
    GridPosition position;
    Candidate candidate;

    public GridCandidate(GridParty party,
                         GridPosition position)
    {
        this.party = party;
        this.position = position;
        candidate = new Candidate(party.getParty(), position.toString());
    }


    public static Comparator<GridCandidate> distanceComparator(GridPosition position)
    {
        return new DistanceComparator<>(position, GridCandidate::getPosition);
    }
}
