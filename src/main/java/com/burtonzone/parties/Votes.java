package com.burtonzone.parties;

import com.burtonzone.common.Decimal;
import java.util.Comparator;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

public class Votes
{
    private final JImmutableMap<Candidate, Decimal> candidates;

    public Votes(JImmutableSet<Candidate> allCandidates)
    {
        candidates = allCandidates.stream()
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

    public Votes without(Candidate candidate)
    {
        return new Votes(candidates.delete(candidate));
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
