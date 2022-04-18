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
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

@Getter
public class Election
{
    private final String region;
    private final JImmutableList<Party> parties;
    private final int seats;
    private final JImmutableList<Candidate> candidates;
    private final JImmutableListMap<Party, Candidate> partyLists;
    private final JImmutableList<Candidate> auxiliaryCandidates;
    private final BallotBox ballots;
    private final Decimal totalVotes;
    private final Decimal quota;
    private final Comparator<Candidate> tieBreaker;

    public Election(String region,
                    JImmutableList<Party> parties,
                    JImmutableList<Candidate> candidates,
                    JImmutableList<Candidate> auxiliaryCandidates,
                    JImmutableListMap<Party, Candidate> partyLists,
                    BallotBox ballots,
                    int seats)
    {
        this.region = region;
        this.parties = parties;
        this.seats = seats;
        this.candidates = candidates;
        this.auxiliaryCandidates = auxiliaryCandidates;
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
            .plus(ONE)
            .rounded(RoundingMode.DOWN);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Counter<Party> getPartyVotes()
    {
        return ballots.getPartyVotes();
    }

    public JImmutableList<Candidate> getExpandedCandidateList()
    {
        return candidates.insertAll(auxiliaryCandidates);
    }

    public static class Builder
    {
        private String region = "";
        private final Set<Party> parties = new LinkedHashSet<>();
        private final BallotBox.Builder ballots = BallotBox.builder();
        private Counter<Party> partyVotes = new Counter<>();
        private JImmutableSet<Candidate> candidates = insertOrderSet();
        private JImmutableSet<Candidate> auxiliaryCandidates = insertOrderSet();
        private int seats = 1;

        public Election build()
        {
            var partyLists = candidates.stream()
                .map(c -> entry(c.getParty(), c))
                .collect(listMapCollector());
            return new Election(region,
                                JImmutables.list(parties),
                                JImmutables.list(auxiliaryCandidates),
                                JImmutables.list(candidates),
                                partyLists,
                                ballots.build(),
                                seats);
        }

        @CanIgnoreReturnValue
        public Builder region(String region)
        {
            this.region = region;
            return this;
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
            candidates = candidates.insert(candidate);
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
            return ballot(count, new Ballot(list(candidates), candidates[0].getParty()));
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
            candidates = candidates.insertAll(ballot.getCandidates());
            return this;
        }
    }
}
