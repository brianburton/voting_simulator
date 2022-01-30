package com.burtonzone.grid;

import com.burtonzone.common.Rand;
import com.burtonzone.election.Ballot;
import com.burtonzone.election.BallotBox;
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
    private static final int MaxVoterDistance = 25;
    private static final int MaxCandidateDistance = 10;
    private static final int MinPartyDistance = 25;
    private static final int VotersPerSeat = 500;
    private static final int VoterTolerance = 20;
    private static final Position Center = new Position((MinPos + MaxPos) / 2, (MinPos + MaxPos) / 2);
    private static final JImmutableList<Integer> PartyPoints = JImmutables.list(20, 30, 40, 50, 60, 70, 80);

    private final Rand rand;
    private final JImmutableList<GridParty> parties;

    public GridElectionFactory(Rand rand,
                               int numParties)
    {
        this.rand = rand;
        this.parties = createParties(rand, numParties);
    }

    @Override
    public Election createElection(int numSeats)
    {
        final var candidates = createCandidates(numSeats);
        final var voterCenter = randomPartyPosition(rand, 3);
        final var ballotBox = createBallotBox(candidates, voterCenter, numSeats);
        return new Election(candidates.transform(GridCandidate::getCandidate), ballotBox, numSeats);
    }

    @Override
    public JImmutableList<Party> allParties()
    {
        return parties.transform(GridParty::getParty);
    }

    private static JImmutableList<GridParty> createParties(Rand rand,
                                                           int numParties)
    {
        var loops = 0;
        var positions = JImmutables.sortedSet(new Position.DistanceComparator(Center));
        while (positions.size() < numParties) {
            if (loops++ >= 100) {
                loops = 0;
                positions = positions.deleteAll();
            }
            final Position position = randomPartyPosition(rand, 1);
            final var minDistance = positions.stream()
                .mapToInt(p -> p.realDistance(position))
                .min()
                .orElse(MaxPos);
            if (positions.isEmpty() || minDistance >= MinPartyDistance) {
                positions = positions.insert(position);
            }
        }
        return positions.transform(JImmutables.list(), p -> new GridParty(p, Center.realDistance(p)));
    }

    private static Position randomPartyPosition(Rand rand,
                                                int bias)
    {
        return new Position(rand.nextElement(PartyPoints, bias),
                            rand.nextElement(PartyPoints, bias));
    }

    private BallotBox createBallotBox(JImmutableList<GridCandidate> candidates,
                                      Position voterCenter,
                                      int numSeats)
    {
        final var numVoters = numSeats * VotersPerSeat;
        final var ballotBox = BallotBox.builder();
        while (ballotBox.count() < numVoters) {
            final var voterPosition = voterCenter
                .nearBy(rand, MaxVoterDistance)
                .wrapped(MinPos, MaxPos);
            final var ballot = createBallot(candidates, voterPosition);
            if (ballot.isNonEmpty()) {
                ballotBox.add(ballot);
            }
        }
        return ballotBox.build();
    }

    private Ballot createBallot(JImmutableList<GridCandidate> candidates,
                                Position position)
    {
        var maxDistance = VoterTolerance * VoterTolerance;
        var choices = candidates.stream()
            .sorted(new GridCandidate.DistanceComparator(position))
            .filter(c -> c.getPosition().quickDistance(position) <= maxDistance)
            .map(GridCandidate::getCandidate)
            .collect(JImmutables.listCollector());
        return new Ballot(choices);
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
        final Position center = party.getPosition();
        var positions = JImmutables.<Position>set();
        while (positions.size() < numCandidates) {
            final Position position = center.centeredNearBy(rand, MaxCandidateDistance);
            positions = positions.insert(position);
        }
        return positions.transform(JImmutables.list(), p -> new GridCandidate(party, p));
    }
}
