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
    public boolean isValidPartyPosition(Position pos)
    {
        final var gridPos = (GridPosition)pos;
        final int x = gridPos.getX();
        final int y = gridPos.getY();
        return (x >= 10 && x <= 90) && (y >= 10 && y <= 90);
    }

    @Override
    public Position centristPartyPosition()
    {
        return new GridPosition(rand.nextInt(35, 65, PartyPositionBias),
                                rand.nextInt(35, 65, PartyPositionBias));
    }

    @Override
    public Position anyPartyPosition()
    {
        return new GridPosition(rand.nextInt(15, 85, PartyPositionBias),
                                rand.nextInt(15, 85, PartyPositionBias));
    }

    @Override
    public Position centerOf(JImmutableList<Position> positions)
    {
        return GridPosition.centerOf(positions);
    }
}
