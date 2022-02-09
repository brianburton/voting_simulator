package com.burtonzone.grid;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.Party;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class GridElectionFactory
    implements ElectionFactory
{
    private static final int MinPos = 0;
    private static final int MaxPos = 100;
    private static final int MaxVoterDistance = 45;
    private static final int MaxCandidateDistance = 10;
    private static final int MinPartyDistance = 15;
    private static final int VotersPerSeat = 500;
    private static final int VoterTolerance = 25;
    private static final int ElectionCenterBias = 3;
    private static final int PartyPositionBias = 1;
    private static final int VoterPositionBias = 4;
    private static final int CandidatePositionBias = 1;
    private static final GridPosition Center = new GridPosition((MinPos + MaxPos) / 2, (MinPos + MaxPos) / 2);
    private static final JImmutableList<Integer> PartyPoints = list(10, 20, 30, 40, 50, 60, 70, 80, 90);
    private static final JImmutableList<GridPosition> StartingPartyPositions = list(
        new GridPosition(40, 40),
        new GridPosition(40, 50),
        new GridPosition(40, 60),
        new GridPosition(50, 40),
        new GridPosition(50, 60),
        new GridPosition(60, 40),
        new GridPosition(60, 50),
        new GridPosition(60, 60)
    );

    private final Rand rand;
    private final JImmutableList<GridParty> parties;

    public GridElectionFactory(Rand rand,
                               int numParties)
    {
        this.rand = rand;
        this.parties = createParties(numParties);
    }

    @Override
    public Election createElection(int numSeats)
    {
        final var candidates = createCandidates(numSeats);
        final var voterCenter = randomCenterPosition();
        final var ballotBox = createBallotBox(candidates, voterCenter, numSeats);
        return new Election(parties.transform(GridParty::getParty), candidates.transform(GridCandidate::getCandidate), ballotBox, numSeats);
    }

    @Override
    public JImmutableList<Party> allParties()
    {
        return parties.transform(GridParty::getParty);
    }

    private JImmutableList<GridParty> createParties(int numParties)
    {
        var loops = 0;
        var positions = JImmutables.sortedSet(new GridPosition.DistanceComparator(Center));
        while (positions.size() < numParties) {
            if (loops++ >= 100) {
                loops = 0;
                positions = positions.deleteAll();
            }
            final GridPosition position;

            if (positions.size() < 2) {
                position = rand.nextElement(StartingPartyPositions);
            } else {
                position = randomPartyPosition();
            }
            final var minDistance = positions.stream()
                .mapToInt(p -> p.realDistance(position))
                .min()
                .orElse(MaxPos);
            if (positions.isEmpty() || minDistance >= MinPartyDistance) {
                positions = positions.insert(position);
            }
        }
        return positions.transform(list(), p -> new GridParty(p, Center.realDistance(p)));
    }

    private GridPosition randomPartyPosition()
    {
        return new GridPosition(rand.nextElement(PartyPoints, PartyPositionBias),
                                rand.nextElement(PartyPoints, PartyPositionBias));
    }

    private GridPosition randomCenterPosition()
    {
        return new GridPosition(rand.nextInt(MinPos, MaxPos, ElectionCenterBias),
                                rand.nextInt(MinPos, MaxPos, ElectionCenterBias));
    }

    private BallotBox createBallotBox(JImmutableList<GridCandidate> candidates,
                                      GridPosition voterCenter,
                                      int numSeats)
    {
        final var numVoters = numSeats * VotersPerSeat;
        final var ballotBox = BallotBox.builder();
        while (ballotBox.count() < numVoters) {
            final var voterPosition = voterCenter
                .centeredNearBy(rand, MaxVoterDistance, VoterPositionBias)
                .wrapped(MinPos, MaxPos);
            final var ballot = createBallot(candidates, voterPosition);
            if (ballot.isNonEmpty()) {
                ballotBox.add(ballot);
            }
        }
        return ballotBox.build();
    }

    private JImmutableList<Candidate> createBallot(JImmutableList<GridCandidate> candidates,
                                                   GridPosition position)
    {
        var maxDistance = GridPosition.toQuickDistance(VoterTolerance);
        var choices = candidates.stream()
            .sorted(new GridCandidate.DistanceComparator(position))
            .filter(c -> c.getPosition().quickDistanceTo(position) <= maxDistance)
            .map(GridCandidate::getCandidate)
            .collect(JImmutables.listCollector());
        return choices;
    }

    private JImmutableList<GridCandidate> createCandidates(int numCandidatesPerParty)
    {
        var candidates = JImmutables.<GridCandidate>list();
        for (GridParty party : parties) {
            var partyCandidates = createPartyCandidates(party, numCandidatesPerParty);
            candidates = candidates.insertAll(partyCandidates);
        }
        return candidates;
    }

    private JImmutableList<GridCandidate> createPartyCandidates(GridParty party,
                                                                int numCandidates)
    {
        final GridPosition center = party.getPosition();
        var positions = JImmutables.<GridPosition>set();
        while (positions.size() < numCandidates) {
            final GridPosition position = center.centeredNearBy(rand, MaxCandidateDistance, CandidatePositionBias);
            positions = positions.insert(position);
        }
        return positions.transform(list(), p -> new GridCandidate(party, p));
    }
}
