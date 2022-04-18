package com.burtonzone.election;

import static com.burtonzone.common.Decimal.ONE;
import static com.burtonzone.common.Decimal.ZERO;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import java.util.Comparator;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;
import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.iterators.TransformIterator;
import org.javimmutable.collections.util.JImmutables;

public class BallotBox
    implements IterableStreamable<Counter.Entry<Ballot>>
{
    private static final JImmutableMap<Ballot, Decimal> NoBallots = JImmutables.map();
    public static final BallotBox Empty = new BallotBox(NoBallots);

    private final JImmutableMap<Ballot, Decimal> ballots;

    private BallotBox(JImmutableMap<Ballot, Decimal> ballots)
    {
        this.ballots = ballots;
    }

    public static Builder builder()
    {
        return new Builder();
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
        for (var e : ballots) {
            count += 1;
            sum += e.getKey().size();
        }
        return (count == 0) ? ZERO : new Decimal(sum).dividedBy(new Decimal(count));
    }

    public BallotBox toSingleChoiceBallots()
    {
        return toPrefixBallots(1);
    }

    public BallotBox toPrefixBallots(int maxRanks)
    {
        var answer = builder();
        for (var ballot : ballots) {
            answer.add(ballot.getKey().prefix(maxRanks), ballot.getValue());
        }
        return answer.build();
    }

    public BallotBox toFirstChoicePartyBallots()
    {
        var answer = builder();
        for (var e : ballots) {
            var choices = e.getKey();
            var firstChoiceParty = choices.first().getParty();
            var filteredChoices = choices.select(c -> c.getParty().equals(firstChoiceParty));
            answer.add(filteredChoices, e.getValue());
        }
        return answer.build();
    }

    public BallotBox toPartyVoteFromFirstChoice()
    {
        var answer = builder();
        for (var e : ballots) {
            answer.add(e.getKey().toPartyVoteFromFirstChoice(), e.getValue());
        }
        return answer.build();
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
                .prefix(numSeats)
                .countPartyVotes()
                .times(e.getValue()))
            .collect(Counter.collectSum());
    }

    public Counter<Party> getPartyVotes()
    {
//        var answer = new Counter<Party>();
//        for (JImmutableMap.Entry<Ballot, Decimal> e : ballots) {
//          answer= answer.add(e.getKey().getParty(), e.getValue());
//        }
//        return answer;
        return ballots.stream()
            .map(e -> entry(e.getKey().getParty(), e.getValue()))
            .collect(Counter.collectEntrySum());
    }

    @Nonnull
    @Override
    public SplitableIterator<Counter.Entry<Ballot>> iterator()
    {
        return TransformIterator.of(ballots.iterator(),
                                    e -> new Counter.Entry<>(e.getKey(), e.getValue()));
    }

    @Override
    public int getSpliteratorCharacteristics()
    {
        return ballots.getSpliteratorCharacteristics();
    }

    public Counter<Candidate> getFirstChoicCandidateVotes()
    {
        return Counter.sum(ballots,
                           e -> e.getKey().first(),
                           e -> e.getValue());
    }

    public Counter<Party> getFirstChoicePartyVotes()
    {
        return Counter.sum(ballots,
                           e -> e.getKey().first().getParty(),
                           e -> e.getValue());
    }

    public Counter<Party> getPartyAllChoiceCounts()
    {
        return ballots.stream()
            .flatMap(e -> e
                .getKey()
                .getCandidates().stream()
                .map(ce -> entry(ce.getParty(), e.getValue())))
            .collect(Counter.collectEntrySum());
    }

    /**
     * Returns a number which is a fraction of the total number of votes in the ballot box.
     * For each ballot the first choice to have a true value from the predicate generates a score.
     * The ballot's first choice scores 1, the second choice scores 1/2, the third scores 1/4 etc.
     *
     * @param isElected return true if the candidate should be considered elected
     * @return the total score for all ballots in the box
     */
    public Decimal getEffectiveVoteScore(Predicate<Candidate> isElected)
    {
        var sum = ZERO;
        for (var ballot : ballots) {
            var divisor = ONE;
            for (Candidate candidate : ballot.getKey().getCandidates()) {
                if (isElected.test(candidate)) {
                    sum = sum.plus(ballot.getValue().dividedBy(divisor));
                    break;
                }
                divisor = divisor.plus(ONE);
            }
        }
        return sum;
    }

    public BallotBox remove(Candidate candidate)
    {
        return removeCandidateImpl(candidate, null);
    }

    public BallotBox removeAndTransfer(Candidate candidate,
                                       Decimal transferWeight)
    {
        if (transferWeight.isZero()) {
            transferWeight = null;
        }
        return removeCandidateImpl(candidate, transferWeight);
    }

    public Decimal getTotalCount()
    {
        return ballots.values().reduce(ZERO, Decimal::plus);
    }

    public boolean isEmpty()
    {
        return ballots.isEmpty();
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

    public Decimal countWastedUsingCandidateOnly(JImmutableList<Candidate> electedCandidates)
    {
        final var electedCandidatesSet = set(electedCandidates);
        return ballots.stream()
            .filter(e -> e.getKey().isWasted(electedCandidatesSet))
            .map(JImmutableMap.Entry::getValue)
            .collect(Decimal.collectSum());
    }

    public Decimal countWastedUsingCandidateOrParty(JImmutableList<Candidate> electedCandidates)
    {
        final var electedCandidatesSet = set(electedCandidates);
        final var electedPartiesSet = electedCandidatesSet.transform(Candidate::getParty);
        return ballots.stream()
            .filter(e -> e.getKey().isWasted(electedCandidatesSet, electedPartiesSet))
            .map(JImmutableMap.Entry::getValue)
            .collect(Decimal.collectSum());
    }

    private BallotBox removeCandidateImpl(Candidate candidate,
                                          @Nullable Decimal transferWeight)
    {
        var newBallots = ballots;
        for (var e : ballots) {
            var oldCandidates = e.getKey();
            var newCandidates = oldCandidates.without(candidate);
            if (newCandidates != oldCandidates) {
                var transferCount = e.getValue();
                if (transferWeight != null && oldCandidates.isFirst(candidate)) {
                    transferCount = transferCount.times(transferWeight);
                }
                newBallots = newBallots.delete(oldCandidates);
                if (!newCandidates.isEmpty()) {
                    final var newCount = transferCount;
                    newBallots = newBallots.update(newCandidates, h -> h.map(newCount::plus).orElse(newCount));
                }
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    /**
     * Removes any ballots with a first choice candidate that matches a predicate.
     * Votes are not exhausted just removed.
     *
     * @param matcher When true causes ballot to be removed.
     * @return new BallotBox
     */
    public BallotBox withoutFirstChoiceMatching(Predicate<Candidate> matcher)
    {
        var newBallots = ballots;
        for (var e : ballots) {
            var ballot = e.getKey();
            if (matcher.test(ballot.first())) {
                newBallots = newBallots.delete(ballot);
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    public BallotBox withoutAnyChoiceMatching(Predicate<Candidate> matcher)
    {
        return withoutPrefixChoiceMatching(Integer.MAX_VALUE, matcher);
    }

    public BallotBox withoutBallotsMatching(Predicate<Ballot> matcher)
    {
        var newBallots = ballots;
        for (JImmutableMap.Entry<Ballot, Decimal> e : newBallots) {
            if (matcher.test(e.getKey())) {
                newBallots = newBallots.delete(e.getKey());
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    /**
     * Removes any ballots with a first prefixLength choice candidate that matches a predicate.
     * Votes are not exhausted just removed.
     *
     * @param prefixLength Number of preferred candidates to test
     * @param matcher      When true causes ballot to be removed.
     * @return new BallotBox
     */
    public BallotBox withoutPrefixChoiceMatching(int prefixLength,
                                                 Predicate<Candidate> matcher)
    {
        var newBallots = ballots;
        for (var e : ballots) {
            var ballot = e.getKey();
            if (ballot.isPrefixMatch(prefixLength, matcher)) {
                newBallots = newBallots.delete(ballot);
            }
        }
        return new BallotBox(newBallots);
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
                var ballotScore = e.getValue();
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
        private JImmutableMap<Ballot, Decimal> ballots;

        private Builder()
        {
            this(NoBallots);
        }

        private Builder(JImmutableMap<Ballot, Decimal> ballots)
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
                ballots = ballots.update(ballot, ballotCount -> ballotCount.map(votes::plus).orElse(votes));
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
