package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import com.burtonzone.parties.Party;
import java.util.Comparator;
import javax.annotation.Nullable;
import lombok.Value;
import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableMultiset;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

public class BallotBox
{
    private final JImmutableMap<Ballot, Integer> ballots;

    private BallotBox(JImmutableMap<Ballot, Integer> ballots)
    {
        this.ballots = ballots;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public IterableStreamable<JImmutableMap.Entry<Ballot, Integer>> ballots()
    {
        return ballots;
    }

    public JImmutableMultiset<Party> getPartyFirstChoiceCounts()
    {
        var answer = JImmutables.<Party>multiset();
        for (JImmutableMap.Entry<Ballot, Integer> e : ballots) {
            answer = answer.insert(e.getKey().getFirstChoice().getParty(), e.getValue());
        }
        return answer;
    }

    public BallotBox remove(Candidate candidate)
    {
        return removeCandidateImpl(candidate, null);
    }

    public BallotBox removeAndTransfer(Candidate candidate,
                                       Decimal extraWeight)
    {
        return removeCandidateImpl(candidate, extraWeight);
    }

    public int getTotalCount()
    {
        return ballots.values().stream().mapToInt(i -> i).sum();
    }

    public boolean isEmpty()
    {
        return ballots.isEmpty();
    }

    public JImmutableSet<Candidate> getCandidates()
    {
        return ballots
            .keys().stream()
            .flatMap(b -> b.getChoices().stream())
            .collect(JImmutables.sortedSetCollector());
    }

    public Comparator<Candidate> createCandidateComparator()
    {
        return new CandidateComparator(ballots);
    }

    private BallotBox removeCandidateImpl(Candidate candidate,
                                          @Nullable Decimal winnerWeight)
    {
        JImmutableMap<Ballot, Integer> newBallots = ballots;
        for (JImmutableMap.Entry<Ballot, Integer> e : ballots) {
            var ballot = e.getKey();
            var newBallot = ballot.without(candidate);
            if (newBallot != ballot) {
                if (winnerWeight != null && ballot.startsWith(candidate)) {
                    newBallot = newBallot.weighted(winnerWeight);
                }
                newBallots = newBallots.delete(ballot);
                if (!newBallot.isEmpty()) {
                    var oldCount = newBallots.find(newBallot).orElse(0);
                    newBallots = newBallots.assign(newBallot, oldCount + e.getValue());
                }
            }
        }
        return newBallots == ballots ? this : new BallotBox(newBallots);
    }

    public boolean isExhausted()
    {
        return ballots.stream().map(b -> b.getKey().ranks()).noneMatch(r -> r > 1);
    }

    public static class CandidateComparator
        implements Comparator<Candidate>
    {
        private final JImmutableMap<Key, Decimal> scores;
        private final int maxSize;

        private CandidateComparator(JImmutableMap<Ballot, Integer> ballots)
        {
            var maxSize = 0;
            var scores = JImmutables.<Key, Decimal>map();
            for (JImmutableMap.Entry<Ballot, Integer> e : ballots) {
                var ballot = e.getKey();
                var count = e.getValue();
                var weight = ballot.getWeight();
                var ballotScore = weight.times(count);
                var choices = ballot.getChoices();
                maxSize = Math.max(maxSize, choices.size());
                for (int i = 0; i < choices.size(); ++i) {
                    var candidate = choices.get(i);
                    var key = new Key(candidate, i);
                    var oldScore = scores.find(key).orElse(Decimal.ZERO);
                    var newScore = oldScore.plus(ballotScore);
                    scores.assign(key, newScore);
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
            return scores.find(key).orElse(Decimal.ZERO);
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
        private JImmutableMap<Ballot, Integer> ballots = JImmutables.map();

        public Builder add(Ballot ballot,
                           int count)
        {
            int oldCount = ballots.getValueOr(ballot, 0);
            ballots = ballots.assign(ballot, oldCount + count);
            return this;
        }

        public BallotBox build()
        {
            return new BallotBox(ballots);
        }
    }
}
