package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.javimmutable.collections.JImmutableList;

@Value
@With
@Builder
public class ElectionSettings
{
    public enum VoteType
    {
        Candidate,
        Party,
        SinglePartyCandidates,
        Mixed
    }

    @Builder.Default
    int numberOfSeats = 1;

    @Builder.Default
    VoteType voteType = VoteType.Candidate;

    @Builder.Default
    int maxCandidateChoices = Integer.MAX_VALUE;

    @Builder.Default
    int maxPartyChoices = 2;

    @Builder.Default
    int mixedPartyVotePercentage = 75;

    @Builder.Default
    JImmutableList<Party> parties = list();
}
