package com.burtonzone.rcv.model;

import static org.javimmutable.collections.util.JImmutables.*;

import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMultiset;
import org.javimmutable.collections.util.JImmutables;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Round
{
    @EqualsAndHashCode.Include
    private final JImmutableMultiset<Ballot> ballots;
    @Getter
    private final JImmutableList<CandidateVotes> counts;
    @Getter
    private final int totalVotes;
    @Nullable
    private final Round prior;

    private Round(JImmutableMultiset<Ballot> ballots)
    {
        this.ballots = ballots;
        counts = countVotes(ballots);
        totalVotes = ballots.occurrenceCount();
        prior = null;
    }

    private Round(Round prior,
                  Candidate... losers)
    {
        ballots = prior.ballots.entries()
            .transform(JImmutables.list(), e -> entry(e.getKey().withoutCandidate(losers), e.getValue()))
            .select(e -> e.getKey().isValid())
            .reduce(JImmutables.multiset(), (m, e) -> m.insert(e.getKey(), e.getValue()));
        counts = countVotes(ballots);
        totalVotes = ballots.occurrenceCount();
        this.prior = prior;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public boolean hasPrior()
    {
        return prior != null;
    }

    public Round prior()
    {
        if (!hasPrior()) {
            throw new NoSuchElementException();
        }
        return prior;
    }

    public boolean hasNext()
    {
        if (isMajority()) {
            return false;
        }
        if (counts.size() < 3) {
            return false;
        }
        if (!getLastPlace().isSingle()) {
            return false;
        }
        return true;
    }

    public Round next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return advance();
    }

    public Round advance()
    {
        return new Round(this, getLastPlace().getSingle());
    }

    public Round solve()
    {
        var r = this;
        while (r.hasNext()) {
            r = r.next();
        }
        return r;
    }

    public int getMajorityVotes()
    {
        return totalVotes / 2 + 1;
    }

    public boolean isMajority()
    {
        return getFirstPlace().votes >= getMajorityVotes();
    }

    public JImmutableList<Round> toList()
    {
        JImmutableList<Round> answer = list();
        Round round = this;
        while (round != null) {
            answer = answer.insertFirst(round);
            round = round.prior;
        }
        return answer;
    }

    public int getCandidateCount()
    {
        return (int)counts.stream()
            .flatMap(cv -> cv.candidates.stream())
            .distinct()
            .count();
    }

    public CandidateVotes getFirstPlace()
    {
        return counts.get(0);
    }

    public CandidateVotes getLastPlace()
    {
        return counts.get(counts.size() - 1);
    }

    private JImmutableList<CandidateVotes> countVotes(JImmutableMultiset<Ballot> ballots)
    {
        var candidateVotes = ballots.entries()
            .reduce(JImmutables.<Candidate>sortedMultiset(),
                    (m, e) -> m.insert(e.getKey().getFirstChoice(), e.getValue()));
        var votesMap = candidateVotes.entries().stream()
            .map(e -> entry(e.getValue(), e.getKey()))
            .collect(JImmutables.listMapCollector());
        return votesMap.keys().stream()
            .map(v -> new CandidateVotes(v, votesMap.get(v)))
            .sorted()
            .collect(JImmutables.listCollector());
    }

    @Value
    public static class CandidateVotes
        implements Comparable<CandidateVotes>
    {
        int votes;
        JImmutableList<Candidate> candidates;

        public boolean isSingle()
        {
            return candidates.size() == 1;
        }

        public Candidate getSingle()
        {
            if (!isSingle()) {
                throw new NoSuchElementException("count is not one");
            }
            return candidates.iterator().next();
        }

        @Override
        public int compareTo(CandidateVotes o)
        {
            return Integer.compare(o.votes, votes);
        }

        @Override
        public String toString()
        {
            return list(votes, candidates).toString();
        }
    }

    public static class Builder
    {
        private JImmutableMultiset<Ballot> ballots = JImmutables.multiset();

        public Builder insert(Ballot ballot)
        {
            validateBallot(ballot);
            ballots = ballots.insert(ballot);
            return this;
        }

        public Builder insertAll(Iterable<Ballot> iterable)
        {
            iterable.forEach(this::validateBallot);
            ballots = ballots.insertAll(iterable);
            return this;
        }

        public Builder insertRepeat(int count,
                                    Ballot ballot)
        {
            validateBallot(ballot);
            ballots = ballots.insert(ballot, count);
            return this;
        }

        public Round build()
        {
            if (ballots.isEmpty()) {
                throw new IllegalArgumentException("no ballot shave been defined");
            }
            return new Round(ballots);
        }

        private void validateBallot(Ballot ballot)
        {
            if (!ballot.isValid()) {
                throw new IllegalArgumentException(String.format("invalid ballot: %s", ballot));
            }
        }
    }
}
