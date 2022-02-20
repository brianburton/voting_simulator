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
    private final Mode mode;

    private enum Mode
    {
        One,
        All,
        Limited
    }

    private PluralityRunner(Mode mode)
    {
        this.mode = mode;
    }

    public static PluralityRunner singleVote()
    {
        return new PluralityRunner(Mode.One);
    }

    public static PluralityRunner blockVote()
    {
        return new PluralityRunner(Mode.All);
    }

    public static PluralityRunner limitedVote()
    {
        return new PluralityRunner(Mode.Limited);
    }

    @Override
    public ElectionResult runElection(Election election)
    {
        final int maxChoices = switch (mode) {
            case One -> 1;
            case All -> election.getSeats();
            case Limited -> (election.getSeats() + 1) / 2;
        };
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
        final var wasted = election.getBallots()
            .withoutPrefixChoiceMatching(maxChoices, electedSet::contains)
            .getTotalCount();
        final var round = new ElectionResult.RoundResult(votes, elected);
        return new ElectionResult(election, list(round), effectiveBallots, wasted);
    }
}