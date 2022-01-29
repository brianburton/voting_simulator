package com.burtonzone.grid;

import com.burtonzone.common.Rand;
import com.burtonzone.election.Ballot;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.Party;
import java.util.ArrayList;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class GridElectionFactory
    implements ElectionFactory
{
    private static final int MinPos = 0;
    private static final int MaxPos = 100;
    private static final int VoterDistance = 25;
    private static final int CandidateDistance = 15;
    private static final int VotersPerSeat = 250;
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
        var candidates = createCandidates(numSeats);
        var voterCenter = randomPartyPosition(rand, 3);
        var voters = createVoters(voterCenter, VotersPerSeat * numSeats);
        var ballotBox = createBallotBox(candidates, voters);
        return new Election(ballotBox, numSeats);
    }

    @Override
    public JImmutableList<Party> allParties()
    {
        return parties.transform(GridParty::getParty);
    }

    private static JImmutableList<GridParty> createParties(Rand rand,
                                                           int numParties)
    {
        var positions = JImmutables.<Position>sortedSet(new Position.DistanceComparator(Center));
        while (positions.size() < numParties) {
            final Position position = randomPartyPosition(rand, 1);
            positions = positions.insert(position);
        }
        return positions.transform(JImmutables.list(), GridParty::new);
    }

    private static Position randomPartyPosition(Rand rand,
                                                int bias)
    {
        return new Position(rand.nextElement(PartyPoints, bias),
                            rand.nextElement(PartyPoints, bias));
    }

    private BallotBox createBallotBox(JImmutableList<GridCandidate> candidates,
                                      JImmutableList<GridVoters> voters)
    {
        var builder = BallotBox.builder();
        for (GridVoters voter : voters) {
            var ballot = createBallot(candidates, voter.getPosition());
            builder.add(ballot, voter.getCount());
        }
        return builder.build();
    }

    private Ballot createBallot(JImmutableList<GridCandidate> candidates,
                                Position position)
    {
        var sorted = new ArrayList<>(candidates.getList());
        sorted.sort(new GridCandidate.DistanceComparator(position));
        final JImmutableList<Candidate> choices = sorted.stream()
            .map(GridCandidate::getCandidate)
            .collect(JImmutables.listCollector());
        return new Ballot(choices);
    }

    private JImmutableList<GridCandidate> createCandidates(int numCandidatesPerParty)
    {
        var candidates = JImmutables.<GridCandidate>list();
        for (GridParty party : parties) {
            var partyCandidates = createCandidates(party, numCandidatesPerParty);
            candidates = candidates.insertAll(partyCandidates);
        }
        return candidates;
    }

    private JImmutableList<GridCandidate> createCandidates(GridParty party,
                                                           int numCandidates)
    {
        final Position center = party.getPosition();
        var positions = JImmutables.<Position>set();
        while (positions.size() < numCandidates) {
            final Position position = center.centeredNearBy(rand, CandidateDistance);
            positions = positions.insert(position);
        }
        return positions.transform(JImmutables.list(), p -> new GridCandidate(party, p));
    }

    private JImmutableList<GridVoters> createVoters(Position centerPosition,
                                                    int numVoters)
    {
        var counts = JImmutables.<Position>multiset();
        for (int i = 1; i <= numVoters; ++i) {
            var position = centerPosition
                .nearBy(rand, VoterDistance)
                .wrapped(MinPos, MaxPos);
            counts = counts.insert(position);
        }
        return counts.entries().stream()
            .map(e -> new GridVoters(e.getKey(), e.getValue()))
            .collect(JImmutables.listCollector());
    }
}
