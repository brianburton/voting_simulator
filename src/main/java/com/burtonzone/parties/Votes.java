package com.burtonzone.parties;

import com.burtonzone.common.Decimal;
import java.util.stream.Stream;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

public class Votes
{
    private final JImmutableMap<Candidate, Decimal> candidates;

    public Votes(Stream<Candidate> allCandidates)
    {
        candidates = allCandidates
            .distinct()
            .map(c -> JImmutables.entry(c, Decimal.ZERO))
            .collect(JImmutables.mapCollector());
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

    public JImmutableList<CandidateVotes> getSortedList()
    {
        return candidates.stream()
            .map(e -> new CandidateVotes(e.getKey(), e.getValue()))
            .collect(JImmutables.listCollector());
    }

    public CandidateVotes getFirst()
    {
        return getSortedList().get(0);
    }

    public CandidateVotes getLast()
    {
        return getSortedList().get(candidates.size() - 1);
    }
}
