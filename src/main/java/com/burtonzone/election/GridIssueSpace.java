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
    public PartyPosition center()
    {
        return GridPosition.Center;
    }

    @Override
    public PartyPosition centristPartyPosition()
    {
        return new GridPosition(rand.nextElement(CenterPartyPoints, PartyPositionBias),
                                rand.nextElement(CenterPartyPoints, PartyPositionBias));
    }

    @Override
    public PartyPosition anyPartyPosition()
    {
        return new GridPosition(rand.nextElement(PartyPoints, PartyPositionBias),
                                rand.nextElement(PartyPoints, PartyPositionBias));
    }

    @Override
    public PartyPosition centerOf(JImmutableList<PartyPosition> positions)
    {
        return GridPosition.centerOf(positions);
    }
}
