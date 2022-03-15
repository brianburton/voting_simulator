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
    public Position center()
    {
        return LinearPosition.Center;
    }

    @Override
    public boolean isValidPartyPosition(Position pos)
    {
        final var linearPos = (LinearPosition)pos;
        final var x = linearPos.getX();
        return x >= 10 && x <= 90;
    }

    @Override
    public Position centristPartyPosition()
    {
        return new LinearPosition(rand.nextElement(CenterPartyPoints, PartyPositionBias));
    }

    @Override
    public Position anyPartyPosition()
    {
        return new LinearPosition(rand.nextElement(PartyPoints, PartyPositionBias));
    }

    @Override
    public Position centerOf(JImmutableList<Position> positions)
    {
        return LinearPosition.centerOf(positions);
    }
}
