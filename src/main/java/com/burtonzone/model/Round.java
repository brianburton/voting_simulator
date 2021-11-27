package com.burtonzone.model;

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
    private final Round prior;

    public Round(Iterable<Ballot> allBallots)
    {
        ballots = JImmutables.multiset(allBallots);
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

    public boolean hasPrior()
    {
        return prior != null;
    }

    @Nullable
    public Round prior()
    {
        return prior;
    }

    public Round next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return new Round(this, counts.get(counts.size() - 1).getSingle());
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
        return counts.get(0).votes >= getMajorityVotes();
    }

    public boolean hasNext()
    {
        if (isMajority()) {
            return false;
        }
        if (counts.size() < 3) {
            return false;
        }
        if (counts.get(counts.size() - 1).candidates.size() > 1) {
            return false;
        }
        return true;
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
}
