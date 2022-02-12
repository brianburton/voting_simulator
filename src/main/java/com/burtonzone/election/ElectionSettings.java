package com.burtonzone.election;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class ElectionSettings
{
    public enum VoteType
    {
        Candidate,
        Party
    }

    @Builder.Default
    int numberOfSeats = 1;

    @Builder.Default
    VoteType voteType = VoteType.Candidate;

    @Builder.Default
    int maxCandidateChoices = 7;

    @Builder.Default
    int maxPartyChoices = 2;
}
