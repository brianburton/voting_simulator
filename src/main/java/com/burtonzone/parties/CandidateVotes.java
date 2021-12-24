package com.burtonzone.parties;

import com.burtonzone.common.Decimal;
import java.util.Comparator;
import lombok.Value;
import lombok.With;

@Value
public class CandidateVotes
{
    Candidate candidate;
    @With
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
