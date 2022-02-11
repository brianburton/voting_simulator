package com.burtonzone.election;

import static com.burtonzone.election.PartyPosition.*;

import com.burtonzone.common.Rand;

public class LinearIssueSpace
    implements IssueSpace
{
    private static final int MaxVoterDistance = 45;
    private static final int ElectionCenterBias = 3;
    private static final int PartyPositionBias = 1;
    private static final int VoterPositionBias = 4;

    private final Rand rand;

    public LinearIssueSpace(Rand rand)
    {
        this.rand = rand;
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
    public PartyPosition voterCenterPosition()
    {
        return new LinearPosition(rand.nextInt(MinPos, MaxPos, ElectionCenterBias));
    }

    @Override
    public PartyPosition voterPosition(PartyPosition voterCenterPosition)
    {
        return ((LinearPosition)voterCenterPosition)
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);
    }

    @Override
    public PartyPosition candidatePosition(PartyPosition partyPosition)
    {
        return ((LinearPosition)partyPosition)
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);

    }

}
