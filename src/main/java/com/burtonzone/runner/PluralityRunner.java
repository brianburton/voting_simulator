package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;

public class PluralityRunner
    implements ElectionRunner

{
    private final int maxChoices;

    private PluralityRunner(int maxChoices)
    {
        this.maxChoices = maxChoices;
    }

    public static PluralityRunner singleVote()
    {
        return new PluralityRunner(1);
    }

    public static PluralityRunner blockVote()
    {
        return new PluralityRunner(Integer.MAX_VALUE);
    }

    @Override
    public ElectionResult runElection(Election election)
    {
        final int maxChoices = Math.min(this.maxChoices, election.getSeats());
        final var effectiveBallots = election.getBallots().toPrefixBallots(maxChoices);
        var counter = new Counter<Candidate>();
        for (var ballot : effectiveBallots) {
            for (Candidate candidate : ballot.getKey()) {
                var count = ballot.getCount();
                counter = counter.add(candidate, count);
            }
        }
        final var votes = counter
            .getSortedList(election.getTieBreaker())
            .slice(0, election.getSeats())
            .transform(CandidateVotes::new);
        final var elected = votes.transform(CandidateVotes::getCandidate);
        final var electedSet = set(elected);
        final var exhausted = election.getBallots()
            .withoutPrefixChoiceMatching(maxChoices, electedSet::contains)
            .getTotalCount();
        final var round = new ElectionResult.RoundResult(votes, elected, exhausted);
        return new ElectionResult(election, list(round), effectiveBallots);
    }
}
