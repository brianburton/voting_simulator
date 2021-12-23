package com.burtonzone.parties;

import com.burtonzone.common.Decimal;
import java.util.Comparator;
import lombok.Value;
import lombok.With;

@Value
public class CandidateVotes
{
    public static Comparator<CandidateVotes> VoteOrder = Comparator
        .comparing(CandidateVotes::getVotes)
        .reversed()
        .thenComparing(CandidateVotes::getCandidate);

    Candidate candidate;
    @With
    Decimal votes;
}
