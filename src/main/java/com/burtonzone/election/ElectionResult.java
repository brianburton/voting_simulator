package com.burtonzone.election;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMultiset;
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

    public JImmutableMultiset<Party> getPartyElectedCounts()
    {
        return getFinalRound().getElected().stream()
            .map(Candidate::getParty)
            .collect(JImmutables.multisetCollector());
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
