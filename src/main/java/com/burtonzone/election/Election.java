package com.burtonzone.election;

import com.burtonzone.common.Decimal;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.math.RoundingMode;
import java.util.Comparator;
import lombok.Getter;
import org.javimmutable.collections.util.JImmutables;

public class Election
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

    public Election(BallotBox ballots,
                    int seats)
    {
        this.seats = seats;
        this.ballots = ballots;
        totalVotes = ballots.getTotalCount();
        quota = totalVotes.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        tieBreaker = ballots.createCandidateComparator();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final BallotBox.Builder ballots = BallotBox.builder();
        private int seats = 1;

        public Election build()
        {
            return new Election(ballots.build(), seats);
        }

        @CanIgnoreReturnValue
        public Builder seats(int val)
        {
            seats = val;
            assert seats > 0;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder ballot(Candidate... candidates)
        {
            return ballot(1, candidates);
        }

        @CanIgnoreReturnValue
        public Builder ballot(int count,
                              Candidate... candidates)
        {
            var ballot = new Ballot(JImmutables.list(candidates));
            return ballot(count, ballot);
        }

        @CanIgnoreReturnValue
        public Builder ballot(Ballot ballot)
        {
            return ballot(1, ballot);
        }

        @CanIgnoreReturnValue
        public Builder ballot(int count,
                              Ballot ballot)
        {
            assert count >= 1;
            ballots.add(ballot, count);
            return this;
        }
    }
}
