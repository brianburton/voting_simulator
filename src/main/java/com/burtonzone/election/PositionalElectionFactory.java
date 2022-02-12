package com.burtonzone.election;

import static com.burtonzone.election.PartyPosition.MaxPos;
import static org.javimmutable.collections.util.JImmutables.*;

import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.util.JImmutables;

public class PositionalElectionFactory
    implements ElectionFactory
{
    private static final int MinPartyDistance = 10;
    private static final int VotersPerSeat = 500;
    private static final int VoterTolerance = PartyPosition.toSquaredDistance(20);

    private final IssueSpace issueSpace;
    private final PartyPosition center;

    public PositionalElectionFactory(IssueSpace issueSpace)
    {
        this.issueSpace = issueSpace;
        this.center = issueSpace.center();
    }

    @Override
    public Election createElection(ElectionSettings settings)
    {
        final var parties = settings.getParties();
        final int numSeats = settings.getNumberOfSeats();
        final var candidates = createCandidates(parties, numSeats);
        final var partyLists = createPartyLists(parties, candidates);
        final var voterCenter = issueSpace.voterCenterPosition();
        final var ballotBox = createBallotBox(parties, candidates, partyLists, voterCenter, settings);
        return new Election(parties, candidates, ballotBox, numSeats);
    }

    @Override
    public JImmutableList<Party> createParties(int numParties)
    {
        var loops = 0;
        var positions = JImmutables.sortedSet(new PartyPosition.DistanceComparator(center));
        while (positions.size() < numParties) {
            if (loops++ >= 100) {
                loops = 0;
                positions = positions.deleteAll();
            }
            final PartyPosition position;

            if (positions.size() < 2) {
                position = issueSpace.centristPartyPosition();
            } else {
                position = issueSpace.anyPartyPosition();
            }
            final var minDistance = positions.stream()
                .mapToInt(p -> p.distanceTo(position).toInt())
                .min()
                .orElse(MaxPos);
            if (positions.isEmpty() || minDistance >= MinPartyDistance) {
                positions = positions.insert(position);
            }
        }
        return positions.transform(list(), p -> new Party(String.format("%s-%d", p, center.distanceTo(p).toInt()), p.toString(), p));
    }

    private BallotBox createBallotBox(JImmutableList<Party> parties,
                                      JImmutableList<Candidate> candidates,
                                      JImmutableListMap<Party, Candidate> partyLists,
                                      PartyPosition voterCenter,
                                      ElectionSettings settings)
    {
        final var numSeats = settings.getNumberOfSeats();
        final var numVoters = numSeats * VotersPerSeat;
        final var ballotBox = BallotBox.builder();
        while (ballotBox.count() < numVoters) {
            final var voterPosition = issueSpace.voterPosition(voterCenter);
            final var ballot =
                settings.getVoteType() == ElectionSettings.VoteType.Candidate
                ? createCandidateOrientedBallot(candidates, voterPosition, settings)
                : createPartyOrientedBallot(parties, partyLists, voterPosition, settings.getMaxPartyChoices());
            if (ballot.isNonEmpty()) {
                ballotBox.add(ballot);
            }
        }
        return ballotBox.build();
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

    private JImmutableList<Candidate> createPartyOrientedBallot(JImmutableList<Party> parties,
                                                                JImmutableListMap<Party, Candidate> partyLists,
                                                                PartyPosition voterPosition,
                                                                int maxParties)
    {
        return parties.stream()
            .filter(p -> p.getPosition().squaredDistanceTo(voterPosition) <= VoterTolerance)
            .sorted(Party.distanceComparator(voterPosition))
            .limit(maxParties)
            .flatMap(p -> partyLists.getList(p).stream())
            .collect(listCollector());
    }

    private JImmutableList<Candidate> createCandidates(JImmutableList<Party> parties,
                                                       int numCandidatesPerParty)
    {
        var candidates = JImmutables.<Candidate>list();
        for (Party party : parties) {
            var partyCandidates = createPartyCandidates(party, numCandidatesPerParty);
            candidates = candidates.insertAll(partyCandidates);
        }
        return candidates;
    }

    private JImmutableList<Candidate> createPartyCandidates(Party party,
                                                            int numCandidates)
    {
        final PartyPosition center = party.getPosition();
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
}
