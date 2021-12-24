package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import javax.annotation.Nullable;
import lombok.Getter;
import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

public class BallotBox
{
    private final JImmutableMap<Ballot, Integer> ballots;
    @Getter
    private final int maxRanks;

    private BallotBox(JImmutableMap<Ballot, Integer> ballots)
    {
        this.ballots = ballots;
        maxRanks = ballots.keys().stream().mapToInt(Ballot::ranks).max().orElse(0);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public IterableStreamable<JImmutableMap.Entry<Ballot, Integer>> ballots()
    {
        return ballots;
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

    public int compareCandidates(Candidate a,
                                 Candidate b)
    {
        for (int index = 0; index < maxRanks; ++index) {
            var aScore = scoreAtIndex(a, index);
            var bScore = scoreAtIndex(b, index);
            var diff = aScore.compareTo(bScore);
            if (diff != 0) {
                return -diff;
            }
        }
        return 0;
    }

    private Decimal scoreAtIndex(Candidate candidate,
                                 int index)
    {
        Decimal answer = Decimal.ZERO;
        for (JImmutableMap.Entry<Ballot, Integer> e : ballots) {
            var ballot = e.getKey();
            if (ballot.candidateIndex(candidate) == index) {
                answer = answer.plus(ballot.getWeight().times(e.getValue()));
            }
        }
        return answer;
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
