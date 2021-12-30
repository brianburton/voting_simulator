package com.burtonzone.election;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class CandidateVotes
{
    Candidate candidate;
    Decimal votes;

    public CandidateVotes(Counter.Entry<Candidate> e)
    {
        this(e.getKey(), e.getCount());
    }

    @Override
    public String toString()
    {
        return "" + candidate + "=" + votes;
    }

    public static Comparator<CandidateVotes> voteOrder(Comparator<Candidate> tieBreaker)
    {
        return Comparator
            .comparing(CandidateVotes::getVotes)
            .reversed()
            .thenComparing(CandidateVotes::getCandidate, tieBreaker)
            .thenComparing(CandidateVotes::getCandidate);
    }
}
