package com.burtonzone.election;

import static com.burtonzone.common.Decimal.ZERO;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import java.util.Comparator;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Value;
import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.iterators.TransformIterator;
import org.javimmutable.collections.util.JImmutables;

public class BallotBox
    implements IterableStreamable<Counter.Entry<Ballot>>
{
    private static final Counter<Ballot> NoBallots = new Counter<>();
    public static final BallotBox Empty = new BallotBox(NoBallots);

    private final Counter<Ballot> ballots;

    private BallotBox(Counter<Ballot> ballots)
    {
        this.ballots = ballots;
    }

    public static Builder builder()
    {
        return new Builder(NoBallots);
    }

    public Builder editor()
    {
        return new Builder(ballots);
    }

    public BallotBox add(BallotBox other)
    {
        return editor().add(other).build();
    }

    public Decimal getAverageNumberOfChoices()
    {
        int count = 0;
        var sum = 0;
        for (var ballot : ballots.keys()) {
            count += 1;
            sum += ballot.size();
        }
        return (count == 0) ? ZERO : new Decimal(sum).divide(count);
    }

    public BallotBox toSingleChoiceBallots()
    {
        return toPrefixBallots(1);
    }

    public BallotBox toPrefixBallots(int maxRanks)
    {
        var newBallots = ballots.stream()
            .map(e -> entry(e.getKey().prefix(maxRanks), e.getCount()))
            .collect(Counter.collectEntrySum());
        return new BallotBox(newBallots);
    }

    /**
     * Replaces the party choice in all ballots to match the party of the first
     * ranked candidate in that ballot.
     */
    public BallotBox toPartyVoteFromFirstChoice()
    {
        var newBallots = ballots.stream()
            .map(e -> entry(e.getKey().toPartyVoteFromFirstChoice(), e.getCount()))
            .collect(Counter.collectEntrySum());
        return new BallotBox(newBallots);
    }

    /**
     * Add up the votes for each party in the ballots taking at most numSeats choices per ballot.
     *
     * @param numSeats the maximum number of choices to count
     * @return votes for every party
     */
    public Counter<Party> getCandidatePartyVotes(int numSeats)
    {
        return ballots.stream()
            .map(e -> e.getKey()
                .countPartyVotes(numSeats)
                .times(e.getCount()))
            .collect(Counter.collectSum());
    }

    /**
     * Add up the actual party vote in the ballots.
     */
    public Counter<Party> getPartyVotes()
    {
        return ballots.stream()
            .map(e -> entry(e.getKey().getParty(), e.getCount()))
            .collect(Counter.collectEntrySum());
    }

    @Nonnull
    @Override
    public SplitableIterator<Counter.Entry<Ballot>> iterator()
    {
        return TransformIterator.of(ballots.iterator(),
                                    e -> new Counter.Entry<>(e.getKey(), e.getCount()));
    }

    @Override
    public int getSpliteratorCharacteristics()
    {
        return ballots.getSpliteratorCharacteristics();
    }

    public Counter<Candidate> getFirstChoiceCandidateVotes()
    {
        return Counter.sum(ballots,
                           e -> e.getKey().first(),
                           e -> e.getCount());
    }

    public BallotBox removeAndTransfer(Candidate candidate,
                                       Decimal transferWeight)
    {
        var newBallots = ballots;
        for (var e : ballots) {
            var oldCandidates = e.getKey();
            var newCandidates = oldCandidates.without(candidate);
            if (newCandidates == oldCandidates) {
                continue;
            }

            newBallots = newBallots.delete(oldCandidates);
            if (newCandidates.isEmpty()) {
                continue;
            }

            var transferCount = e.getCount();
            if (oldCandidates.isFirst(candidate)) {
                transferCount = transferCount.times(transferWeight);
                if (transferCount.isZero()) {
                    continue;
                }
            }

            final var count = transferCount;
            newBallots = newBallots.add(newCandidates, count);
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    public Decimal getTotalCount()
    {
        return ballots.values().reduce(ZERO, Decimal::plus);
    }

    public JImmutableSet<Candidate> getCandidates()
    {
        return ballots
            .keys()
            .stream()
            .flatMap(ballot -> ballot.getCandidates().stream())
            .collect(JImmutables.sortedSetCollector());
    }

    public Comparator<Candidate> createCandidateComparator()
    {
        return new CandidateComparator();
    }

    public Decimal countWastedUsingCandidateOnly(JImmutableList<CandidateVotes> electedVotes)
    {
        final var electedCandidatesSet = electedVotes.transform(set(), CandidateVotes::getCandidate);
        return ballots.stream()
            .filter(e -> e.getKey().isWasted(electedCandidatesSet))
            .map(ballotEntry -> ballotEntry.getCount())
            .collect(Decimal.collectSum());
    }

    public Decimal countWastedUsingCandidateOrParty(JImmutableList<CandidateVotes> electedVotes)
    {
        final var electedCandidatesSet = electedVotes.transform(set(), CandidateVotes::getCandidate);
        final var electedPartiesSet = electedCandidatesSet.transform(Candidate::getParty);
        return ballots.stream()
            .filter(e -> e.getKey().isWasted(electedCandidatesSet, electedPartiesSet))
            .map(Counter.Entry::getCount)
            .collect(Decimal.collectSum());
    }

    public BallotBox withoutBallotsMatching(Predicate<Ballot> matcher)
    {
        var newBallots = ballots;
        for (Counter.Entry<Ballot> e : newBallots) {
            if (matcher.test(e.getKey())) {
                newBallots = newBallots.delete(e.getKey());
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    private class CandidateComparator
        implements Comparator<Candidate>
    {
        private final Counter<ComparatorKey> scores;
        private final int maxSize;

        private CandidateComparator()
        {
            var maxSize = 0;
            var scores = new Counter<ComparatorKey>();
            for (var e : ballots) {
                var choices = e.getKey().getCandidates();
                var ballotScore = e.getCount();
                maxSize = Math.max(maxSize, choices.size());
                for (int i = 0; i < choices.size(); ++i) {
                    var candidate = choices.get(i);
                    var key = new ComparatorKey(candidate, i);
                    scores = scores.add(key, ballotScore);
                }
            }
            this.scores = scores;
            this.maxSize = maxSize;
        }

        @Override
        public int compare(Candidate a,
                           Candidate b)
        {
            for (int i = 0; i < maxSize; ++i) {
                var aScore = candidateScoreAt(a, i);
                var bScore = candidateScoreAt(b, i);
                var diff = aScore.compareTo(bScore);
                if (diff != 0) {
                    return -diff;
                }
            }
            return 0;
        }

        private Decimal candidateScoreAt(Candidate candidate,
                                         int index)
        {
            var key = new ComparatorKey(candidate, index);
            return scores.get(key);
        }
    }

    @Value
    private static class ComparatorKey
    {
        Candidate candidate;
        int index;
    }

    public static class Builder
    {
        private Counter<Ballot> ballots;

        private Builder(Counter<Ballot> ballots)
        {
            this.ballots = ballots;
        }

        public Builder add(Ballot ballot)
        {
            return add(ballot, 1);
        }

        public Builder add(Ballot ballot,
                           int votes)
        {
            return add(ballot, new Decimal(votes));
        }

        public Builder add(Ballot ballot,
                           Decimal votes)
        {
            if (!ballot.isEmpty()) {
                ballots = ballots.add(ballot, votes);
            }
            return this;
        }

        public Builder add(BallotBox ballots)
        {
            for (Counter.Entry<Ballot> ballot : ballots) {
                add(ballot.getKey(), ballot.getCount());
            }
            return this;
        }

        public BallotBox build()
        {
            return new BallotBox(ballots);
        }
    }
}
