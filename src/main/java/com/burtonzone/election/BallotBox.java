package com.burtonzone.election;

import static com.burtonzone.common.Decimal.*;
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
import org.javimmutable.collections.iterators.TransformStreamable;
import org.javimmutable.collections.util.JImmutables;

public class BallotBox
    implements IterableStreamable<Counter.Entry<JImmutableList<Candidate>>>
{
    private static final JImmutableList<Candidate> NoCandidates = list();
    private static final JImmutableMap<JImmutableList<Candidate>, Decimal> NoBallots =
        JImmutables.<JImmutableList<Candidate>, Decimal>map()
            .assign(NoCandidates, ZERO);

    private final JImmutableMap<JImmutableList<Candidate>, Decimal> ballots;

    private BallotBox(JImmutableMap<JImmutableList<Candidate>, Decimal> ballots)
    {
        assert ballots.get(NoCandidates) != null;
        this.ballots = ballots;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public BallotBox add(BallotBox other)
    {
        var counter = new Counter<>(ballots);
        for (Counter.Entry<JImmutableList<Candidate>> entry : other) {
            counter = counter.add(entry.getKey(), entry.getCount());
        }
        return new BallotBox(counter.toMap());
    }

    public Decimal getAverageNumberOfChoices()
    {
        int count = 0;
        var sum = 0;
        for (var e : ballots) {
            if (e.getKey().isNonEmpty()) {
                count += 1;
                sum += e.getKey().size();
            }
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
            answer.add(ballot.getKey().slice(0, maxRanks), ballot.getValue());
        }
        return answer.build();
    }

    public BallotBox toFirstChoicePartyBallots()
    {
        var answer = builder();
        for (var ballot : ballots) {
            var choices = ballot.getKey();
            if (choices.isNonEmpty()) {
                var firstChoiceParty = ballot.getKey().get(0).getParty();
                var filteredChoices = choices.select(c -> c.getParty().equals(firstChoiceParty));
                answer.add(filteredChoices, ballot.getValue());
            } else {
                answer.add(ballot.getKey(), ballot.getValue());
            }
        }
        return answer.build();
    }

    @Nonnull
    @Override
    public SplitableIterator<Counter.Entry<JImmutableList<Candidate>>> iterator()
    {
        return TransformIterator.of(ballots.iterator(),
                                    e -> new Counter.Entry<>(e.getKey(), e.getValue()));
    }

    @Override
    public int getSpliteratorCharacteristics()
    {
        return ballots.getSpliteratorCharacteristics();
    }

    public IterableStreamable<Counter.Entry<Candidate>> getFirstChoiceCounts()
    {
        return TransformStreamable.of(ballots.delete(NoCandidates),
                                      e -> new Counter.Entry<>(e.getKey().get(0), e.getValue()));
    }

    public Counter<Party> getPartyFirstChoiceCounts()
    {
        var answer = new Counter<Party>();
        for (var e : ballots) {
            final var candidates = e.getKey();
            if (candidates.isNonEmpty()) {
                final var candidate = candidates.get(0);
                answer = answer.add(candidate.getParty(), e.getValue());
            }
        }
        return answer;
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
            for (Candidate candidate : ballot.getKey()) {
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
        // We always have a NoCandidates entry so if that's the only one we're out of candidates.
        return ballots.size() == 1;
    }

    public JImmutableSet<Candidate> getCandidates()
    {
        return ballots
            .keys()
            .stream()
            .flatMap(IterableStreamable::stream)
            .collect(JImmutables.sortedSetCollector());
    }

    public Comparator<Candidate> createCandidateComparator()
    {
        return new CandidateComparator();
    }

    private BallotBox removeCandidateImpl(Candidate candidate,
                                          @Nullable Decimal transferWeight)
    {
        var newBallots = ballots;
        for (var e : ballots) {
            var oldCandidates = e.getKey();
            var newCandidates = oldCandidates.reject(c -> c.equals(candidate));
            if (newCandidates != oldCandidates) {
                var transferCount = e.getValue();
                if (transferWeight != null && oldCandidates.get(0).equals(candidate)) {
                    transferCount = transferCount.times(transferWeight);
                }
                final var oldCount = newBallots.get(newCandidates);
                newBallots = newBallots
                    .delete(oldCandidates)
                    .assign(newCandidates, oldCount == null ? transferCount : oldCount.plus(transferCount));
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    public Decimal getExhaustedCount()
    {
        return ballots.get(NoCandidates);
    }

    /**
     * Add all ballot counts to the exhausted count and remove them.
     *
     * @return new BallotBox
     */
    public BallotBox exhaustAllRemaining()
    {
        return new BallotBox(NoBallots.assign(NoCandidates, getTotalCount()));
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
        var ballots = this.ballots;
        for (var e : this.ballots) {
            var ballot = e.getKey();
            if (ballot.size() > 0 && matcher.test(ballot.get(0))) {
                ballots = ballots.delete(ballot);
            }
        }
        return new BallotBox(ballots);
    }

    public BallotBox withoutAnyChoiceMatching(Predicate<Candidate> matcher)
    {
        return withoutPrefixChoiceMatching(Integer.MAX_VALUE, matcher);
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
        var ballots = this.ballots;
        for (var e : this.ballots) {
            var ballot = e.getKey();
            if (ballot.size() > 0) {
                if (ballot.stream().limit(prefixLength).anyMatch(matcher)) {
                    ballots = ballots.delete(ballot);

                }
            }
        }
        return new BallotBox(ballots);
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
                var choices = e.getKey();
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
        private JImmutableMap<JImmutableList<Candidate>, Decimal> ballots = NoBallots;
        private int count;

        public Builder add(JImmutableList<Candidate> candidates)
        {
            return add(candidates, 1);
        }

        public Builder add(JImmutableList<Candidate> candidates,
                           int count)
        {
            final var votes = new Decimal(count);
            ballots = ballots.update(candidates, c -> c.map(votes::plus).orElse(votes));
            this.count += count;
            return this;
        }

        public Builder add(JImmutableList<Candidate> candidates,
                           Decimal votes)
        {
            ballots = ballots.update(candidates, c -> c.map(votes::plus).orElse(votes));
            this.count += votes.toInt();
            return this;
        }

        public Builder add(BallotBox ballots)
        {
            for (Counter.Entry<JImmutableList<Candidate>> ballot : ballots) {
                add(ballot.getKey(), ballot.getCount());
            }
            return this;
        }

        public int count()
        {
            return count;
        }

        public BallotBox build()
        {
            return new BallotBox(ballots);
        }
    }
}
