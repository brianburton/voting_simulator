package com.burtonzone.election;

import static com.burtonzone.election.Position.*;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import org.javimmutable.collections.JImmutableList;

public abstract class IssueSpace
{
    protected static final int MaxVoterDistance = 45;
    protected static final int PartyPositionBias = 1;
    protected static final int VoterPositionBias = 2;
    protected static final int CandidatePositionBias = 3;
    protected static final JImmutableList<Integer> PartyPoints = list(10, 15, 20, 25, 30, 35, 40, 45, 55, 60, 65, 70, 75, 80, 85, 90);
    protected static final JImmutableList<Integer> CenterPartyPoints = list(35, 40, 45, 55, 60, 65);

    protected final Rand rand;

    protected IssueSpace(Rand rand)
    {
        this.rand = rand;
    }

    public abstract Position centristPartyPosition();

    public abstract Position anyPartyPosition();

    public abstract Position center();

    public abstract Position centerOf(JImmutableList<Position> positions);

    public Position voterCenterPosition(JImmutableList<Party> parties)
    {
        var positions = parties.transform(Party::getPosition);
        if (positions.size() > 2) {
            var center = centerOf(positions);
            var sorted = positions.stream()
                .sorted(new Position.DistanceComparator(center))
                .skip(1)
                .collect(listCollector());
            var preferredParty = sorted.get(rand.nextIndex(sorted.size()));
            positions = positions.insert(preferredParty);
        }

//        final var targetSize = 1 + parties.size() / 2;
//        var positions = list(center());
//        while (positions.size() < targetSize) {
//            final var index = rand.nextIndex(parties.size());
//            final var party = parties.get(index);
//            positions = positions.insert(party.getPosition());
//            parties = parties.delete(index);
//        }

//        final var targetSize = 1 + parties.size() / 2;
//        var positions = parties.transform(Party::getPosition);
//        while (positions.size() > targetSize) {
//            final var index = rand.nextIndex(parties.size());
//            positions = positions.delete(index);
//            parties = parties.delete(index);
//        }

//        var positions = parties.transform(Party::getPosition);
//        if (positions.size() > 3) {
//            var center = centerOf(positions);
//            positions = positions.stream()
//                .sorted(new Position.DistanceComparator(center))
//                .skip(1)
//                .collect(listCollector());
//        }

        return centerOf(positions);
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
