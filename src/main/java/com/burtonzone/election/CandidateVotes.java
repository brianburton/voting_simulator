package com.burtonzone.election;

import com.burtonzone.common.Decimal;
import java.util.Comparator;
import lombok.Value;

@Value
public class CandidateVotes
{
    Candidate candidate;
    Decimal votes;

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
