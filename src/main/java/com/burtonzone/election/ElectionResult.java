package com.burtonzone.election;

import lombok.Value;
import org.javimmutable.collections.JImmutableList;

public class ElectionResult
{
    private final PreElection preElection;
    private final JImmutableList<RoundResult> results;

    public ElectionResult(PreElection preElection,
                          JImmutableList<RoundResult> results)
    {
        assert results.isNonEmpty();
        this.preElection = preElection;
        this.results = results;
    }

    public RoundResult getFinalRound()
    {
        return results.get(results.size() - 1);
    }

    public boolean isComplete()
    {
        return getFinalRound().elected.size() == preElection.getSeats();
    }

    public JImmutableList<Candidate> getElected()
    {
        return getFinalRound().getElected();
    }

    public JImmutableList<CandidateVotes> getVotes()
    {
        return getFinalRound().getVotes();
    }

    @Value
    public static class RoundResult
    {
        JImmutableList<CandidateVotes> votes;
        JImmutableList<Candidate> elected;
    }
}
