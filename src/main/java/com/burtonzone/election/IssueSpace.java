package com.burtonzone.election;

import static com.burtonzone.election.Position.MaxPos;
import static com.burtonzone.election.Position.MinPos;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import org.javimmutable.collections.JImmutableList;

public abstract class IssueSpace
{
    protected static final int MaxVoterDistance = 45;
    protected static final int PartyPositionBias = 1;
    protected static final int VoterPositionBias = 3;
    protected static final int VoterCenterBias = 4;
    protected static final int CandidatePositionBias = 3;
    protected static final JImmutableList<Integer> PartyPoints = list(10, 15, 20, 25, 30, 35, 40, 45, 55, 60, 65, 70, 75, 80, 85, 90);
    protected static final JImmutableList<Integer> CenterPartyPoints = list(35, 40, 45, 55, 60, 65);

    protected final Rand rand;

    protected IssueSpace(Rand rand)
    {
        this.rand = rand;
    }

    public abstract boolean isValidPartyPosition(Position pos);

    public abstract Position centristPartyPosition();

    public abstract Position anyPartyPosition();

    public abstract Position center();

    public abstract Position centerOf(JImmutableList<Position> positions);

    public Position voterCenterPosition(JImmutableList<Party> parties)
    {
        final var centerParties = parties.slice(0, Math.max(2, parties.size() - 1));
        final var positions = centerParties.transform(Party::getPosition);
        return positions.get(0).somewhereIn(rand, VoterCenterBias, positions);
    }

    public Position voterPosition(Position voterCenterPosition)
    {
        return voterCenterPosition
            .nearBy(rand, MaxVoterDistance, VoterPositionBias)
            .wrapped(MinPos, MaxPos);
    }

    public Position candidatePosition(Position position,
                                      int numberOfParties)
    {
        return position
            .nearBy(rand, minPartyDistance(numberOfParties), CandidatePositionBias)
            .wrapped(MinPos, MaxPos);
    }

    public int minPartyDistance(int numberOfParties)
    {
        return Math.min(30, 100 / numberOfParties);
    }

    public int maxPartyDistance(boolean isCore)
    {
        return isCore ? 40 : 85;
    }
}
