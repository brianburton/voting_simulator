package com.burtonzone.model;

import static org.javimmutable.collections.util.JImmutables.*;

import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableMultiset;
import org.javimmutable.collections.util.JImmutables;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Round
{
    @EqualsAndHashCode.Include
    private final JImmutableMultiset<Ballot> ballots;
    private final JImmutableMultiset<Candidate> weights;
    @Getter
    private final JImmutableList<CandidateVotes> counts;
    @Getter
    private final int totalVotes;
    private final Round prior;

    public Round(Iterable<Ballot> allBallots)
    {
        ballots = JImmutables.multiset(allBallots);
        weights = computeCandidateWeights(ballots);
        counts = countVotes(ballots);
        totalVotes = ballots.occurrenceCount();
        prior = null;
    }

    private Round(Round prior)
    {
        final var loser = prior.getLastPlaceCandidate();
        ballots = prior.ballots.entries()
            .transform(JImmutables.list(), e -> entry(e.getKey().withoutCandidate(loser), e.getValue()))
            .select(e -> e.getKey().isValid())
            .reduce(JImmutables.multiset(), (m, e) -> m.insert(e.getKey().withoutCandidate(loser), e.getValue()));
        weights = prior.weights;
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
        return new Round(this);
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
        return !(counts.size() < 2 || isMajority());
    }

    public Candidate getFirstPlaceCandidate()
    {
        return counts.get(0).candidate;
    }

    public Candidate getLastPlaceCandidate()
    {
        return counts.get(counts.size() - 1).candidate;
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

    static JImmutableMultiset<Candidate> computeCandidateWeights(JImmutableMultiset<Ballot> ballots)
    {
        final int maxWeight = ballots.stream()
            .mapToInt(b -> b.getChoices().size())
            .max()
            .orElse(1);
        JImmutableMultiset<Candidate> answer = JImmutables.multiset();
        for (JImmutableMap.Entry<Ballot, Integer> e : ballots.entries()) {
            final var candidates = e.getKey().getChoices();
            final int count = e.getValue();
            for (int i = 0; i < candidates.size(); ++i) {
                var candidate = candidates.get(i);
                var weight = (maxWeight - i) * count;
                answer = answer.insert(candidate, weight);
            }
        }
        return answer;
    }

    private JImmutableList<CandidateVotes> countVotes(JImmutableMultiset<Ballot> ballots)
    {
        var candidateVotes = ballots.entries()
            .reduce(JImmutables.<Candidate>multiset(),
                    (m, e) -> m.insert(e.getKey().getFirstChoice(), e.getValue()));
        return candidateVotes.entries().stream()
            .map(e -> new CandidateVotes(e.getKey(), e.getValue(), weights.count(e.getKey())))
            .sorted()
            .collect(JImmutables.listCollector());
    }

    @Value
    public static class CandidateVotes
        implements Comparable<CandidateVotes>
    {
        Candidate candidate;
        int votes;
        int weight;

        @Override
        public int compareTo(CandidateVotes o)
        {
            var diff = Integer.compare(o.votes, votes);
            if (diff == 0) {
                diff = Integer.compare(o.weight, weight);
            }
            if (diff == 0) {
                diff = candidate.compareTo(o.candidate);
            }
            return diff;
        }

        @Override
        public String toString()
        {
            return list(candidate, votes, weight).toString();
        }
    }
}
