package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Decimal;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.util.JImmutables;

public class Election
{
    @Getter
    private final JImmutableList<Party> parties;
    @Getter
    private final int seats;
    @Getter
    private final JImmutableList<Candidate> candidates;
    @Getter
    private final JImmutableListMap<Party, Candidate> partyLists;
    @Getter
    private final BallotBox ballots;
    @Getter
    private final Decimal totalVotes;
    @Getter
    private final Decimal quota;
    @Getter
    private final Comparator<Candidate> tieBreaker;

    public Election(JImmutableList<Party> parties,
                    JImmutableList<Candidate> candidates,
                    JImmutableListMap<Party, Candidate> partyLists,
                    BallotBox ballots,
                    int seats)
    {
        this.parties = parties;
        this.seats = seats;
        this.candidates = candidates;
        this.partyLists = partyLists;
        this.ballots = ballots;
        totalVotes = ballots.getTotalCount();
        quota = computeQuota(totalVotes, seats);
        tieBreaker = ballots.createCandidateComparator();
    }

    public static Decimal computeQuota(Decimal totalVotes,
                                       int numberOfSeats)
    {
        return totalVotes.dividedBy(new Decimal(numberOfSeats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final Set<Party> parties = new LinkedHashSet<>();
        private final BallotBox.Builder ballots = BallotBox.builder();
        private final Set<Candidate> candidates = new LinkedHashSet<>();
        private int seats = 1;

        public Election build()
        {
            var partyLists = candidates.stream()
                .map(c -> entry(c.getParty(), c))
                .collect(listMapCollector());
            return new Election(JImmutables.list(parties), JImmutables.list(candidates), partyLists, ballots.build(), seats);
        }

        @CanIgnoreReturnValue
        public Builder parties(Iterable<Party> parties)
        {
            for (Party party : parties) {
                this.parties.add(party);
            }
            return this;
        }

        @CanIgnoreReturnValue
        public Builder candidates(Candidate... candidates)
        {
            for (Candidate candidate : candidates) {
                candidate(candidate);
            }
            return this;
        }

        @CanIgnoreReturnValue
        public Builder candidate(Candidate candidate)
        {
            candidates.add(candidate);
            parties.add(candidate.getParty());
            return this;
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
            return ballot(count, JImmutables.list(candidates));
        }

        @CanIgnoreReturnValue
        public Builder ballot(JImmutableList<Candidate> candidates)
        {
            return ballot(1, candidates);
        }

        @CanIgnoreReturnValue
        public Builder ballot(int count,
                              JImmutableList<Candidate> candidates)
        {
            assert count >= 1;
            ballots.add(candidates, count);
            for (Candidate candidate : candidates) {
                this.candidates.add(candidate);
            }
            return this;
        }
    }
}
