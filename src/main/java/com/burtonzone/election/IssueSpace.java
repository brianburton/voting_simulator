package com.burtonzone.election;

import static com.burtonzone.election.PartyPosition.*;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import org.javimmutable.collections.JImmutableList;

public abstract class IssueSpace
{
    protected static final int ElectionCenterBias = 3;
    protected static final int MaxVoterDistance = 45;
    protected static final int MinMaxPartyPositionOffset = 25;
    protected static final int PartyPositionBias = 1;
    protected static final int VoterPositionBias = 4;
    protected static final JImmutableList<Integer> PartyPoints = list(10, 15, 20, 25, 30, 35, 40, 45, 55, 60, 65, 70, 75, 80, 85, 90);
    protected static final JImmutableList<Integer> CenterPartyPoints = list(35, 40, 45, 55, 60, 65);

    protected final Rand rand;

    protected IssueSpace(Rand rand)
    {
        this.rand = rand;
    }

    public abstract PartyPosition centristPartyPosition();

    public abstract PartyPosition anyPartyPosition();

    public abstract PartyPosition center();

    public abstract PartyPosition centerOf(JImmutableList<PartyPosition> positions);

    public PartyPosition voterCenterPosition(JImmutableList<Party> parties)
    {
        final var positions = parties.transform(Party::getPosition);
        final var partyCenter = centerOf(positions);
        final var maxOffset = Math.max(MinMaxPartyPositionOffset, partyCenter.distanceTo(center()).toInt());
        return partyCenter.nearBy(rand, maxOffset, ElectionCenterBias);
    }

    public PartyPosition voterPosition(PartyPosition voterCenterPosition)
    {
        return voterCenterPosition
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);
    }

    public PartyPosition candidatePosition(PartyPosition partyPosition)
    {
        return partyPosition
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);
    }
}
