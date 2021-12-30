package com.burtonzone.election;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import java.util.Comparator;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Votes
{
    private final Counter<Candidate> candidates;

    public Votes()
    {
        candidates = new Counter<>();
    }

    private Votes(Counter<Candidate> candidates)
    {
        this.candidates = candidates;
    }

    public Votes plus(Candidate candidate,
                      Decimal votes)
    {
        return new Votes(candidates.add(candidate, votes));
    }

    public Decimal getVotesFor(Candidate c)
    {
        return candidates.get(c);
    }

    public int size()
    {
        return candidates.size();
    }

    public JImmutableList<CandidateVotes> getSortedList(Comparator<Candidate> tieBreaker)
    {
        var comparator = CandidateVotes.voteOrder(tieBreaker);
        return candidates.stream()
            .map(e -> new CandidateVotes(e.getKey(), e.getCount()))
            .sorted(comparator)
            .collect(JImmutables.listCollector());
    }
}
