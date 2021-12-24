package com.burtonzone.parties;

import com.burtonzone.common.Decimal;
import java.util.Comparator;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

public class Votes
{
    private final JImmutableMap<Candidate, Decimal> candidates;

    public Votes()
    {
        candidates = JImmutables.map();
    }

    private Votes(JImmutableMap<Candidate, Decimal> candidates)
    {
        this.candidates = candidates;
    }

    public Votes plus(Candidate candidate,
                      Decimal votes)
    {
        return new Votes(candidates.assign(candidate, getVotesFor(candidate).plus(votes)));
    }

    public Decimal getVotesFor(Candidate c)
    {
        return candidates.find(c).orElse(Decimal.ZERO);
    }

    public int size()
    {
        return candidates.size();
    }

    public JImmutableList<CandidateVotes> getSortedList(Comparator<Candidate> tieBreaker)
    {
        var comparator = CandidateVotes.voteOrder(tieBreaker);
        return candidates.stream()
            .map(e -> new CandidateVotes(e.getKey(), e.getValue()))
            .sorted(comparator)
            .collect(JImmutables.listCollector());
    }
}
