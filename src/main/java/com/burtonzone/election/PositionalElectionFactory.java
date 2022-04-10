package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Rand;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.util.JImmutables;

public class PositionalElectionFactory
    implements ElectionFactory
{
    private static final int VotersPerSeat = 500;
    private static final int VoterTolerance = Position.toSquaredDistance(25);

    private final Rand rand;
    private final IssueSpace issueSpace;
    private final Position center;
    private final AtomicInteger ids = new AtomicInteger(1);

    public PositionalElectionFactory(Rand rand,
                                     IssueSpace issueSpace)
    {
        this.rand = rand;
        this.issueSpace = issueSpace;
        this.center = issueSpace.center();
    }

    @Override
    public Election createElection(ElectionSettings settings)
    {
        final var parties = settings.getParties();
        final var numSeats = settings.getNumberOfSeats();
        final var numVoters = numSeats * VotersPerSeat;
        final var voterCenter = issueSpace.voterCenterPosition(parties);
        final var voters = createVoters(parties, voterCenter, numVoters);
        final var candidates = createCandidates(parties, numSeats);
        final var auxiliaryCandidates = createCandidates(parties, numSeats);
        final var partyLists = Candidate.createPartyLists(parties, candidates);
        final var ballotBox = createBallotBox(voters, candidates, partyLists, settings);
        final var partyVotes = voters.reduce(new Counter<Party>(), (pv, v) -> pv.inc(v.parties.get(0)));
        return new Election(settings.getRegion(), parties, candidates, auxiliaryCandidates, partyLists, partyVotes, ballotBox, numSeats);
    }

    @Override
    public JImmutableList<Party> createParties(int numParties)
    {
        final var minAllowedDistance = issueSpace.minPartyDistance(numParties);
        final var maxAllowedDistance = minAllowedDistance + issueSpace.maxPartyDistance(false) / 2;
        var loops = 0;
        var positions = list(issueSpace.centristPartyPosition());
        while (positions.size() < numParties) {
            if (loops++ >= 100) {
                loops = 0;
                positions = list(issueSpace.centristPartyPosition());
                System.out.println(".");
            }
            final var offset = rand.nextInt(minAllowedDistance, maxAllowedDistance);
            final var neighbor = rand.nextElement(positions);
            final var newPosition = neighbor.moveDistance(rand, offset);
            if (newPosition.isValid() && issueSpace.isValidPartyPosition(newPosition)) {
                final var newPositions = positions.insert(newPosition);
                final var distances = positions.stream()
                    .map(p -> p.distanceTo(newPosition).toInt())
                    .sorted()
                    .collect(listCollector());
                final var min = distances.get(0);
                final var max = distances.get(distances.size() - 1);
                if (min >= minAllowedDistance && max < maxAllowedDistance) {
                    positions = newPositions;
                }
            }
        }
        return positions.stream()
            .sorted(new Position.DistanceComparator(center))
            .map(p -> new Party(String.format("%s-%d", p, center.distanceTo(p).toInt()), p.toString(), p))
            .collect(listCollector());
    }

    private BallotBox createBallotBox(JImmutableList<Voter> voters,
                                      JImmutableList<Candidate> candidates,
                                      JImmutableListMap<Party, Candidate> partyLists,
                                      ElectionSettings settings)
    {
        final var ballotBox = BallotBox.builder();
        for (Voter voter : voters) {
            var ballot = JImmutables.<Candidate>list();
            if (settings.getVoteType() == ElectionSettings.VoteType.PartyList) {
                ballot = createPartyListBallot(voter, partyLists, settings.getMaxPartyChoices());
            } else if (settings.getVoteType() == ElectionSettings.VoteType.Mixed
                       && rand.nextInt(1, 100) <= settings.getMixedPartyVotePercentage()) {
                ballot = createPartyListBallot(voter, partyLists, settings.getMaxPartyChoices());
            } else if (settings.getVoteType() == ElectionSettings.VoteType.PartyCandidate) {
                ballot = createPartyCandidateBallot(voter, candidates, settings.getMaxPartyChoices());
            } else {
                ballot = createCandidateOrientedBallot(candidates, voter.position, settings);
            }
            if (ballot.isNonEmpty()) {
                ballotBox.add(ballot);
            }
        }
        return ballotBox.build();
    }

    private JImmutableList<Voter> createVoters(JImmutableList<Party> parties,
                                               Position voterCenter,
                                               int maxVoters)
    {
        return Stream.generate(() -> issueSpace.voterPosition(voterCenter))
            .map(position -> new Voter(position,
                                       parties.stream()
                                           .filter(p -> p.getPosition().squaredDistanceTo(position) <= VoterTolerance)
                                           .sorted(Party.distanceComparator(position))
                                           .collect(listCollector())))
            .filter(Voter::isValid)
            .limit(maxVoters)
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createCandidateOrientedBallot(JImmutableList<Candidate> candidates,
                                                                    Position position,
                                                                    ElectionSettings settings)
    {
        final var maxCandidates = Math.min(settings.getMaxCandidateChoices(),
                                           settings.getMaxPartyChoices() * settings.getNumberOfSeats());
        return candidates.stream()
            .filter(c -> c.getPosition().squaredDistanceTo(position) <= VoterTolerance)
            .sorted(Candidate.distanceComparator(position))
            .limit(maxCandidates)
            .collect(JImmutables.listCollector());
    }

    private JImmutableList<Candidate> createPartyCandidateBallot(Voter voter,
                                                                 JImmutableList<Candidate> candidates,
                                                                 int maxParties)
    {
        final var sortedCandidates = candidates.stream()
            .sorted(Candidate.distanceComparator(voter.position))
            .collect(listCollector());
        return voter.parties.stream()
            .limit(maxParties)
            .flatMap(party ->
                         sortedCandidates.stream()
                             .filter(c -> c.getParty().equals(party)))
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createPartyListBallot(Voter voter,
                                                            JImmutableListMap<Party, Candidate> partyLists,
                                                            int maxParties)
    {
        return voter.parties.stream()
            .limit(maxParties)
            .flatMap(p -> partyLists.getList(p).stream())
            .collect(listCollector());
    }

    public JImmutableList<Candidate> createCandidates(JImmutableList<Party> parties,
                                                      int numCandidatesPerParty)
    {
        return parties.stream()
            .flatMap(party -> createPartyCandidates(party, numCandidatesPerParty, parties.size()).stream())
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createPartyCandidates(Party party,
                                                            int numCandidates,
                                                            int numParties)
    {
        var positions = JImmutables.<Position>set();
        while (positions.size() < numCandidates) {
            final Position position = issueSpace.candidatePosition(party.getPosition(), numParties);
            positions = positions.insert(position);
        }
        return positions.transform(list(), p -> new Candidate(party, p.toString() + "-" + ids.getAndIncrement(), p));
    }

    @AllArgsConstructor
    private static class Voter
    {
        Position position;
        JImmutableList<Party> parties;

        private boolean isValid()
        {
            return parties.isNonEmpty();
        }
    }
}
