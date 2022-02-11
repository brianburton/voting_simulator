package com.burtonzone.election;

import static com.burtonzone.election.PartyPosition.*;

import com.burtonzone.common.Rand;

public class GridIssueSpace
    implements IssueSpace
{
    private static final int MaxVoterDistance = 45;
    private static final int ElectionCenterBias = 3;
    private static final int PartyPositionBias = 1;
    private static final int VoterPositionBias = 4;

    private final Rand rand;

    public GridIssueSpace(Rand rand)
    {
        this.rand = rand;
    }

    @Override
    public PartyPosition center()
    {
        return GridPosition.Center;
    }

    @Override
    public GridPosition centristPartyPosition()
    {
        return new GridPosition(rand.nextElement(CenterPartyPoints, PartyPositionBias),
                                rand.nextElement(CenterPartyPoints, PartyPositionBias));
    }

    @Override
    public GridPosition anyPartyPosition()
    {
        return new GridPosition(rand.nextElement(PartyPoints, PartyPositionBias),
                                rand.nextElement(PartyPoints, PartyPositionBias));
    }

    @Override
    public GridPosition voterCenterPosition()
    {
        return new GridPosition(rand.nextInt(MinPos, MaxPos, ElectionCenterBias),
                                rand.nextInt(MinPos, MaxPos, ElectionCenterBias));
    }

    @Override
    public GridPosition voterPosition(PartyPosition voterCenterPosition)
    {
        return ((GridPosition)voterCenterPosition)
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);
    }

    @Override
    public GridPosition candidatePosition(PartyPosition partyPosition)
    {
        return ((GridPosition)partyPosition)
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);

    }
}
