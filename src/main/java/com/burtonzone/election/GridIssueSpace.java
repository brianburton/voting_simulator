package com.burtonzone.election;

import com.burtonzone.common.Rand;
import org.javimmutable.collections.JImmutableList;

public class GridIssueSpace
    extends IssueSpace
{
    public GridIssueSpace(Rand rand)
    {
        super(rand);
    }

    @Override
    public Position center()
    {
        return GridPosition.Center;
    }

    @Override
    public Position centristPartyPosition()
    {
        return new GridPosition(rand.nextElement(CenterPartyPoints, PartyPositionBias),
                                rand.nextElement(CenterPartyPoints, PartyPositionBias));
    }

    @Override
    public Position anyPartyPosition()
    {
        return new GridPosition(rand.nextElement(PartyPoints, PartyPositionBias),
                                rand.nextElement(PartyPoints, PartyPositionBias));
    }

    @Override
    public Position centerOf(JImmutableList<Position> positions)
    {
        return GridPosition.centerOf(positions);
    }
}
