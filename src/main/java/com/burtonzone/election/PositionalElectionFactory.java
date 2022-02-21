package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.util.JImmutables;

public class PositionalElectionFactory
    implements ElectionFactory
{
    private static final int MinPartyDistance = 10;
    private static final int VotersPerSeat = 500;
    private static final int VoterTolerance = PartyPosition.toSquaredDistance(25);

    private final Rand rand;
    private final IssueSpace issueSpace;
    private final PartyPosition center;

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
        final var partyLists = createPartyLists(parties, candidates);
        final var ballotBox = createBallotBox(voters, candidates, partyLists, settings);
        return new Election(parties, candidates, ballotBox, numSeats);
    }

    @Override
    public JImmutableList<Party> createParties(int numParties)
    {
        final var coreSize = numParties <= 5 ? 2 : 3;
        final var minAllowedDistance = Math.min(30, 100 / numParties);
        var loops = 0;
        var positions = list(issueSpace.centristPartyPosition());
        while (positions.size() < numParties) {
            if (loops++ >= 100) {
                loops = 0;
                positions = list(issueSpace.centristPartyPosition());
                System.out.println(".");
            }
            final var newPosition = issueSpace.anyPartyPosition();
            final var newPositions = positions.insert(newPosition);
            final var distances = positions.stream()
                .map(p -> p.distanceTo(newPosition).toInt())
                .sorted()
                .collect(listCollector());
            final var min = distances.get(0);
            final var max = distances.get(distances.size() - 1);
            final var maxAllowedDistance = newPositions.size() <= coreSize ? 40 : 85;
            if (min >= minAllowedDistance && max < maxAllowedDistance) {
                positions = newPositions;
            }
        }
        return positions.stream()
            .sorted(new PartyPosition.DistanceComparator(center))
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
            if (settings.getVoteType() == ElectionSettings.VoteType.Party) {
                ballot = createPartyOrientedBallot(voter, partyLists, settings.getMaxPartyChoices());
            } else if (settings.getVoteType() == ElectionSettings.VoteType.Mixed
                       && rand.nextInt(1, 100) <= settings.getMixedPartyVotePercentage()) {
                ballot = createPartyOrientedBallot(voter, partyLists, settings.getMaxPartyChoices());
            } else if (settings.getVoteType() == ElectionSettings.VoteType.SinglePartyCandidates) {
                ballot = createSinglePartyCandidateOrientedBallot(candidates, voter.position, settings);
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
                                               PartyPosition voterCenter,
                                               int maxVoters)
    {
        return Stream.generate(() -> issueSpace.voterPosition(voterCenter))
            .map(position -> new Voter(position,
                                       parties.stream()
                                           .filter(p -> p.getPosition().squaredDistanceTo(position) <= VoterTolerance)
                                           .sorted(Party.distanceComparator(position))
                                           .collect(listCollector())))
            .limit(maxVoters)
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createCandidateOrientedBallot(JImmutableList<Candidate> candidates,
                                                                    PartyPosition position,
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

    private JImmutableList<Candidate> createSinglePartyCandidateOrientedBallot(JImmutableList<Candidate> candidates,
                                                                               PartyPosition position,
                                                                               ElectionSettings settings)
    {
        final var maxCandidates = settings.getMaxCandidateChoices();
        final var sortedCandidates = candidates.stream()
            .sorted(Candidate.distanceComparator(position))
            .collect(listCollector());
        final var firstParty = sortedCandidates.get(0).getParty();
        return sortedCandidates.stream()
            .filter(c -> c.getParty().equals(firstParty))
            .limit(maxCandidates)
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createPartyOrientedBallot(Voter voter,
                                                                JImmutableListMap<Party, Candidate> partyLists,
                                                                int maxParties)
    {
        return voter.parties.stream()
            .limit(maxParties)
            .flatMap(p -> partyLists.getList(p).stream())
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createCandidates(JImmutableList<Party> parties,
                                                       int numCandidatesPerParty)
    {
        return parties.stream()
            .flatMap(party -> createPartyCandidates(party, numCandidatesPerParty).stream())
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createPartyCandidates(Party party,
                                                            int numCandidates)
    {
        var positions = JImmutables.<PartyPosition>set();
        while (positions.size() < numCandidates) {
            final PartyPosition position = issueSpace.candidatePosition(party.getPosition());
            positions = positions.insert(position);
        }
        return positions.transform(list(), p -> new Candidate(party, p.toString(), p));
    }

    private JImmutableListMap<Party, Candidate> createPartyLists(JImmutableList<Party> parties,
                                                                 JImmutableList<Candidate> candidates)
    {
        JImmutableListMap<Party, Candidate> answer = listMap();
        for (Party party : parties) {
            var sorted = candidates.stream()
                .filter(c -> c.getParty().equals(party))
                .sorted(Candidate.distanceComparator(party.getPosition()))
                .collect(listCollector());
            answer = answer.assign(party, sorted);
        }
        return answer;
    }

    @AllArgsConstructor
    private static class Voter
    {
        PartyPosition position;
        JImmutableList<Party> parties;
    }
}
