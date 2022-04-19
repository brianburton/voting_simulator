package com.burtonzone.election;

import static com.burtonzone.common.Decimal.ZERO;

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

    public ElectionResult(Election election,
                          JImmutableList<RoundResult> results,
                          BallotBox effectiveBallots,
                          Counter<Party> partyVotes,
                          Decimal wasted)
    {
        assert results.isNonEmpty();
        this.election = election;
        this.results = results;
        this.effectiveBallots = effectiveBallots;
        this.partyVotes = partyVotes;
        this.wasted = wasted;
    }

    public Election getElection()
    {
        return election;
    }

    public RoundResult getFinalRound()
    {
        return results.get(results.size() - 1);
    }

    public BallotBox getEffectiveBallots()
    {
        return effectiveBallots;
    }

    public JImmutableList<Candidate> getElected()
    {
        return getFinalRound().getElected();
    }

    public JImmutableList<CandidateVotes> getVotes()
    {
        return getFinalRound().getVotes();
    }

    public Decimal getWasted()
    {
        return wasted;
    }

    public Counter<Party> getPartyElectedCounts()
    {
        return Counter.count(getFinalRound().getElected(), Candidate::getParty);
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
        var sum = new Counter<Party>();
        for (CandidateVotes candidateVotes : getFinalRound().getVotes()) {
            if (candidateVotes.isList()) {
                sum = sum.inc(candidateVotes.getCandidate().getParty());
            }
        }
        return sum;
    }

    @Value
    public static class RoundResult
    {
        JImmutableList<CandidateVotes> votes;
        JImmutableList<Candidate> elected;

        public int getSeats()
        {
            return elected.size();
        }
    }
}
