package com.burtonzone.election;

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

    public ElectionResult(Election election,
                          JImmutableList<RoundResult> results,
                          BallotBox effectiveBallots)
    {
        assert results.isNonEmpty();
        this.election = election;
        this.results = results;
        this.effectiveBallots = effectiveBallots;
    }

    public ElectionResult(Election election,
                          JImmutableList<RoundResult> results)
    {
        this(election, results, election.getBallots());
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

    public Counter<Party> getPartyFirstChoiceCounts()
    {
        return election.getBallots().getPartyFirstChoiceCounts();
    }

    public Counter<Party> getPartyElectedCounts()
    {
        var answer = new Counter<Party>();
        for (Candidate candidate : getFinalRound().getElected()) {
            answer = answer.add(candidate.getParty(), Decimal.ONE);
        }
        return answer;
    }

    public Decimal getEffectiveVoteScore()
    {
        final var candidateSet = JImmutables.set(getElected());
        return effectiveBallots.getEffectiveVoteScore(candidateSet::contains);
    }

    @Value
    public static class RoundResult
    {
        JImmutableList<CandidateVotes> votes;
        JImmutableList<Candidate> elected;
        Decimal exhausted;

        public int getSeats()
        {
            return elected.size();
        }
    }
}
