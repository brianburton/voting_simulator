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
    private final Decimal wasted;

    public ElectionResult(Election election,
                          JImmutableList<RoundResult> results,
                          BallotBox effectiveBallots,
                          Decimal wasted)
    {
        assert results.isNonEmpty();
        this.election = election;
        this.results = results;
        this.effectiveBallots = effectiveBallots;
        this.wasted = wasted;
    }

    public static ElectionResult ofPartyListResults(Election election,
                                                    JImmutableList<CandidateVotes> electedCandidates)
    {
        final var elected = electedCandidates.transform(CandidateVotes::getCandidate);
        final var electedParties = elected.transform(set(), Candidate::getParty);
        final var wasted = election.getBallots()
            .withoutFirstChoiceMatching(c -> electedParties.contains(c.getParty()))
            .getTotalCount();
        final var round = new ElectionResult.RoundResult(electedCandidates, elected);
        final var effectiveBallots = election.getBallots().toFirstChoicePartyBallots();
        return new ElectionResult(election, list(round), effectiveBallots, wasted);
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

    public boolean isComplete()
    {
        return getFinalRound().elected.size() == election.getSeats();
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

    public Counter<Party> getPartyEffectiveVoteCounts()
    {
        return effectiveBallots.getPartyAllChoiceCounts();
    }

    public Counter<Party> getPartyElectedCounts()
    {
        var answer = new Counter<Party>();
        for (Candidate candidate : getFinalRound().getElected()) {
            answer = answer.inc(candidate.getParty());
        }
        return answer;
    }

    public Counter<Party> getPartyVoteCounts()
    {
        return effectiveBallots.getPartyVoteCounts(election.getSeats());
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
        final var partyVotes = getPartyEffectiveVoteCounts();
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
