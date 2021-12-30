package com.burtonzone.election;

import com.burtonzone.common.Decimal;
import java.math.RoundingMode;
import java.util.Comparator;
import lombok.Getter;

public class PreElection
{
    @Getter
    private final int seats;
    @Getter
    private final BallotBox ballots;
    @Getter
    private final Decimal totalVotes;
    @Getter
    private final Decimal quota;
    @Getter
    private final Comparator<Candidate> tieBreaker;

    public PreElection(int seats,
                       BallotBox ballots)
    {
        this.seats = seats;
        this.ballots = ballots;
        totalVotes = ballots.getTotalCount();
        quota = totalVotes.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        tieBreaker = ballots.createCandidateComparator();
    }
}
