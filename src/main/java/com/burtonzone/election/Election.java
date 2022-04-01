package com.burtonzone.election;

import static com.burtonzone.common.Decimal.ONE;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
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

@Getter
public class Election
{
    private final JImmutableList<Party> parties;
    private final int seats;
    private final JImmutableList<Candidate> candidates;
    private final JImmutableListMap<Party, Candidate> partyLists;
    private final Counter<Party> partyVotes;
    private final JImmutableList<Candidate> auxiliaryCandidates;
    private final BallotBox ballots;
    private final Decimal totalVotes;
    private final Decimal quota;
    private final Comparator<Candidate> tieBreaker;

    public Election(JImmutableList<Party> parties,
                    JImmutableList<Candidate> candidates,
                    JImmutableList<Candidate> auxiliaryCandidates,
                    JImmutableListMap<Party, Candidate> partyLists,
                    Counter<Party> partyVotes,
                    BallotBox ballots,
                    int seats)
    {
        this.parties = parties;
        this.seats = seats;
        this.candidates = candidates;
        this.auxiliaryCandidates = auxiliaryCandidates;
        this.partyLists = partyLists;
        this.partyVotes = partyVotes;
        this.ballots = ballots;
        totalVotes = ballots.getTotalCount();
        quota = computeQuota(totalVotes, seats);
        tieBreaker = ballots.createCandidateComparator();
    }

    public static Decimal computeQuota(Decimal totalVotes,
                                       int numberOfSeats)
    {
        return totalVotes.dividedBy(new Decimal(numberOfSeats + 1))
            .plus(ONE)
            .rounded(RoundingMode.DOWN);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public JImmutableList<Candidate> getExpandedCandidateList()
    {
        return candidates.insertAll(auxiliaryCandidates);
    }

    public static class Builder
    {
        private final Set<Party> parties = new LinkedHashSet<>();
        private final BallotBox.Builder ballots = BallotBox.builder();
        private Counter<Party> partyVotes = new Counter<>();
        private final Set<Candidate> candidates = new LinkedHashSet<>();
        private final Set<Candidate> auxiliaryCandidates = new LinkedHashSet<>();
        private int seats = 1;

        public Election build()
        {
            var partyLists = candidates.stream()
                .map(c -> entry(c.getParty(), c))
                .collect(listMapCollector());
            return new Election(JImmutables.list(parties), JImmutables.list(auxiliaryCandidates), JImmutables.list(candidates), partyLists, partyVotes, ballots.build(), seats);
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

        @CanIgnoreReturnValue
        public Builder partyVote(Party party)
        {
            partyVotes = partyVotes.inc(party);
            return this;
        }
    }
}
