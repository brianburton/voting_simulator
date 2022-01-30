package com.burtonzone.election;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import java.util.Comparator;
import javax.annotation.Nullable;
import lombok.Value;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

public class BallotBox
{
    private final Counter<Ballot> ballots;

    private BallotBox(Counter<Ballot> ballots)
    {
        this.ballots = ballots;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Counter<Ballot> ballots()
    {
        return ballots;
    }

    public Counter<Party> getPartyFirstChoiceCounts()
    {
        var answer = new Counter<Party>();
        for (Counter.Entry<Ballot> e : ballots) {
            if (!e.getKey().isEmpty()) {
                final var candidate = e.getKey().getFirstChoice();
                answer = answer.add(candidate.getParty(), e.getCount());
            }
        }
        return answer;
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
        return ballots.getTotal();
    }

    public boolean isEmpty()
    {
        return ballots.stream().map(Counter.Entry::getKey).noneMatch(Ballot::isNonEmpty);
    }

    public JImmutableSet<Candidate> getCandidates()
    {
        return ballots
            .stream()
            .flatMap(e -> e.getKey().getChoices().stream())
            .collect(JImmutables.sortedSetCollector());
    }

    public Comparator<Candidate> createCandidateComparator()
    {
        return new CandidateComparator(ballots);
    }

    private BallotBox removeCandidateImpl(Candidate candidate,
                                          @Nullable Decimal transferWeight)
    {
        Counter<Ballot> newBallots = ballots;
        for (Counter.Entry<Ballot> e : ballots) {
            var ballot = e.getKey();
            var newBallot = ballot.without(candidate);
            if (newBallot != ballot) {
                var count = e.getCount();
                if (transferWeight != null && ballot.startsWith(candidate)) {
                    count = count.times(transferWeight);
                }
                newBallots = newBallots.delete(ballot);
                newBallots = newBallots.add(newBallot, count);
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    public boolean isExhausted()
    {
        return ballots.stream().map(b -> b.getKey().ranks()).noneMatch(r -> r > 1);
    }

    public Decimal getExhaustedCount()
    {
        return ballots.get(Ballot.Empty);
    }

    public static class CandidateComparator
        implements Comparator<Candidate>
    {
        private final Counter<Key> scores;
        private final int maxSize;

        private CandidateComparator(Counter<Ballot> ballots)
        {
            var maxSize = 0;
            var scores = new Counter<Key>();
            for (Counter.Entry<Ballot> e : ballots) {
                var ballot = e.getKey();
                var ballotScore = e.getCount();
                var choices = ballot.getChoices();
                maxSize = Math.max(maxSize, choices.size());
                for (int i = 0; i < choices.size(); ++i) {
                    var candidate = choices.get(i);
                    var key = new Key(candidate, i);
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
            var key = new Key(candidate, index);
            return scores.get(key);
        }

        @Value
        private static class Key
        {
            Candidate candidate;
            int index;
        }
    }

    public static class Builder
    {
        private Counter<Ballot> ballots = new Counter<>();
        private int count;

        public Builder add(Ballot ballot)
        {
            return add(ballot, 1);
        }

        public Builder add(Ballot ballot,
                           int count)
        {
            ballots = ballots.add(ballot, count);
            this.count += count;
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
