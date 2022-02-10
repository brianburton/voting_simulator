package com.burtonzone.grid;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionSettings;
import com.burtonzone.election.Party;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.util.JImmutables;

public class GridElectionFactory
    implements ElectionFactory
{
    private static final int MinPos = 0;
    private static final int MaxPos = 100;
    private static final int MaxVoterDistance = 45;
    private static final int MaxCandidateDistance = 10;
    private static final int MinPartyDistance = 10;
    private static final int VotersPerSeat = 500;
    private static final int VoterTolerance = GridPosition.toQuickDistance(25);
    private static final int ElectionCenterBias = 3;
    private static final int PartyPositionBias = 1;
    private static final int VoterPositionBias = 4;
    private static final int CandidatePositionBias = 1;
    private static final GridPosition Center = new GridPosition((MinPos + MaxPos) / 2, (MinPos + MaxPos) / 2);
    private static final JImmutableList<Integer> PartyPoints = list(10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90);
    private static final JImmutableList<Integer> StartingPartyPoints = list(40, 45, 50, 55, 60);

    private final Rand rand;
    private final JImmutableList<GridParty> parties;

    public GridElectionFactory(Rand rand,
                               int numParties)
    {
        this.rand = rand;
        this.parties = createParties(numParties);
    }

    @Override
    public Election createElection(ElectionSettings settings)
    {
        final int numSeats = settings.getNumberOfSeats();
        final var candidates = createCandidates(numSeats);
        final var partyLists = createPartyLists(candidates);
        final var voterCenter = randomCenterPosition();
        final var ballotBox = createBallotBox(candidates, partyLists, voterCenter, settings);
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
                position = randomPartyPosition(StartingPartyPoints);
            } else {
                position = randomPartyPosition(PartyPoints);
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

    private GridPosition randomPartyPosition(JImmutableList<Integer> points)
    {
        return new GridPosition(rand.nextElement(points, PartyPositionBias),
                                rand.nextElement(points, PartyPositionBias));
    }

    private GridPosition randomCenterPosition()
    {
        return new GridPosition(rand.nextInt(MinPos, MaxPos, ElectionCenterBias),
                                rand.nextInt(MinPos, MaxPos, ElectionCenterBias));
    }

    private BallotBox createBallotBox(JImmutableList<GridCandidate> candidates,
                                      JImmutableListMap<GridParty, GridCandidate> partyLists,
                                      GridPosition voterCenter,
                                      ElectionSettings settings)
    {
        final var numSeats = settings.getNumberOfSeats();
        final var numVoters = numSeats * VotersPerSeat;
        final var ballotBox = BallotBox.builder();
        while (ballotBox.count() < numVoters) {
            final var voterPosition = voterCenter
                .centeredNearBy(rand, MaxVoterDistance, VoterPositionBias)
                .wrapped(MinPos, MaxPos);
            final var ballot =
                settings.getVoteType() == ElectionSettings.VoteType.Candidate
                ? createCandidateOrientedBallot(candidates, voterPosition, settings.getMaxCandidateChoices())
                : createPartyOrientedBallot(partyLists, voterPosition, settings.getMaxPartyChoices());
            if (ballot.isNonEmpty()) {
                ballotBox.add(ballot);
            }
        }
        return ballotBox.build();
    }

    private JImmutableList<Candidate> createCandidateOrientedBallot(JImmutableList<GridCandidate> candidates,
                                                                    GridPosition position,
                                                                    int maxCandidates)
    {
        return candidates.stream()
            .filter(c -> c.getPosition().quickDistanceTo(position) <= VoterTolerance)
            .sorted(GridCandidate.distanceComparator(position))
            .limit(maxCandidates)
            .map(GridCandidate::getCandidate)
            .collect(JImmutables.listCollector());
    }

    private JImmutableList<Candidate> createPartyOrientedBallot(JImmutableListMap<GridParty, GridCandidate> partyLists,
                                                                GridPosition voterPosition,
                                                                int maxParties)
    {
        return parties.stream()
            .filter(p -> p.getPosition().quickDistanceTo(voterPosition) <= VoterTolerance)
            .sorted(GridParty.distanceComparator(voterPosition))
            .limit(maxParties)
            .flatMap(p -> partyLists.getList(p).stream())
            .map(GridCandidate::getCandidate)
            .collect(listCollector());
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

    private JImmutableListMap<GridParty, GridCandidate> createPartyLists(JImmutableList<GridCandidate> candidates)
    {
        JImmutableListMap<GridParty, GridCandidate> answer = listMap();
        for (GridParty party : parties) {
            var sorted = candidates.stream()
                .filter(c -> c.getParty().equals(party))
                .sorted(GridCandidate.distanceComparator(party.getPosition()))
                .collect(listCollector());
            answer = answer.assign(party, sorted);
        }
        return answer;
    }
}
