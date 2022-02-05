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

    public ElectionResult(Election election,
                          JImmutableList<RoundResult> results)
    {
        assert results.isNonEmpty();
        this.election = election;
        this.results = results;
    }

    public Election getElection()
    {
        return election;
    }

    public RoundResult getFinalRound()
    {
        return results.get(results.size() - 1);
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

    /**
     * @return Number of voters whose first choice candidate was elected.
     */
    public int getEffectiveFirstVoteCount()
    {
        final var candidateSet = JImmutables.set(getElected());
        var count = 0;
        for (Counter.Entry<Ballot> e : election.getBallots().ballots()) {
            final var ballot = e.getKey();
            final var votes = e.getCount();
            final var candidate = ballot.getFirstChoice();
            if (candidateSet.contains(candidate)) {
                count = count + votes.toInt();
            }
        }
        return count;
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
