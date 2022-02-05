package com.burtonzone.grid;

import com.burtonzone.election.Candidate;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
public class GridCandidate
{
    GridParty party;
    Position position;
    Candidate candidate;

    public GridCandidate(GridParty party,
                         Position position)
    {
        this.party = party;
        this.position = position;
        candidate = new Candidate(party.getParty(), position.toString());
    }

    @AllArgsConstructor
    public static class DistanceComparator
        implements Comparator<GridCandidate>
    {
        private final Position position;

        @Override
        public int compare(GridCandidate a,
                           GridCandidate b)
        {
            var distA = position.quickDistanceTo(a.getPosition());
            var distB = position.quickDistanceTo(b.getPosition());
            return distA - distB;
        }
    }
}
