package com.burtonzone.election;

import com.burtonzone.common.Rand;
import org.javimmutable.collections.JImmutableList;

public class LinearIssueSpace
    extends IssueSpace
{
    public LinearIssueSpace(Rand rand)
    {
        super(rand);
    }

    @Override
    public PartyPosition center()
    {
        return LinearPosition.Center;
    }

    @Override
    public PartyPosition centristPartyPosition()
    {
        return new LinearPosition(rand.nextElement(CenterPartyPoints, PartyPositionBias));
    }

    @Override
    public PartyPosition anyPartyPosition()
    {
        return new LinearPosition(rand.nextElement(PartyPoints, PartyPositionBias));
    }

    @Override
    public PartyPosition centerOf(JImmutableList<PartyPosition> positions)
    {
        return LinearPosition.centerOf(positions);
    }
}
