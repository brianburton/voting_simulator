package com.burtonzone.election;

import static com.burtonzone.common.Decimal.ZERO;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class ElectionResult
{
    private final Election election;
    private final JImmutableList<RoundResult> results;
    private final BallotBox effectiveBallots;
    private final Counter<Party> partyVotes;
    private final Decimal wasted;
    private final JImmutableList<CandidateVotes> electedVotes;

    public ElectionResult(Election election,
                          JImmutableList<RoundResult> results,
                          BallotBox effectiveBallots,
                          Counter<Party> partyVotes,
                          Decimal wasted)
    {
        this.election = election;
        this.results = results;
        this.effectiveBallots = effectiveBallots;
        this.partyVotes = partyVotes;
        this.wasted = wasted;
        electedVotes = results.stream()
            .flatMap(r -> r.getVotes().stream())
            .collect(listCollector());
    }

    public Election getElection()
    {
        return election;
    }

    public JImmutableList<RoundResult> getResults()
    {
        return results;
    }

    public BallotBox getEffectiveBallots()
    {
        return effectiveBallots;
    }

    public JImmutableList<Candidate> getElected()
    {
        return electedVotes.transform(CandidateVotes::getCandidate);
    }

    public JImmutableList<CandidateVotes> getVotes()
    {
        return electedVotes;
    }

    public int getElectedCount()
    {
        return electedVotes.size();
    }

    public Decimal getWasted()
    {
        return wasted;
    }

    public Counter<Party> getPartyElectedCounts()
    {
        return electedVotes.stream()
            .map(cv -> cv.getCandidate().getParty())
            .collect(Counter.collectCounts());
    }

    public Counter<Party> getPartyVoteCounts()
    {
        return partyVotes;
    }

    public Decimal getEffectiveVoteScore()
    {
        final var candidateSet = JImmutables.set(getElected());
        return effectiveBallots.getEffectiveVoteScore(candidateSet::contains);
    }

    // https://en.wikipedia.org/wiki/Gallagher_index
    public Decimal computeErrors()
    {
        final var partySeats = getPartyElectedCounts();
        final var totalSeats = partySeats.getTotal();
        final var totalVotes = partyVotes.getTotal();
        var sum = ZERO;
        for (Party party : getElection().getParties()) {
            final var seatPercentage = partySeats.get(party).dividedBy(totalSeats);
            final var votePercentage = partyVotes.get(party).dividedBy(totalVotes);
            final var diffSquared = votePercentage.minus(seatPercentage).squared();
            sum = sum.plus(diffSquared);
        }
        return sum.dividedBy(Decimal.TWO).root();
    }

    public Counter<Party> getPartyListSeats()
    {
        return electedVotes.stream()
            .filter(CandidateVotes::isList)
            .map(cv -> cv.getCandidate().getParty())
            .collect(Counter.collectCounts());
    }

    @Value
    public static class RoundResult
    {
        JImmutableList<CandidateVotes> votes;

        public int getSeats()
        {
            return votes.size();
        }

        public JImmutableList<Candidate> getElected()
        {
            return votes.transform(CandidateVotes::getCandidate);
        }
    }
}
