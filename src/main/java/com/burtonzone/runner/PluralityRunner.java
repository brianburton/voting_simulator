package com.burtonzone.runner;

import static com.burtonzone.common.Decimal.ZERO;
import static com.burtonzone.election.CandidateVotes.SelectionType.Vote;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import org.javimmutable.collections.JImmutableSet;

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
            for (Candidate candidate : ballot.getKey().getCandidates()) {
                var count = ballot.getCount();
                counter = counter.add(candidate, count);
            }
        }
        final var votes = counter
            .getSortedList(election.getTieBreaker())
            .slice(0, election.getSeats())
            .transform(cv -> new CandidateVotes(cv, Vote));
        final var elected = CandidateVotes.toCandidateSet(votes);
        final var wasted = election.getBallots().countWastedUsingCandidateOnly(votes);
        final var effectiveVoteScore = computeEffectiveVoteScore(effectiveBallots, elected);
        return new ElectionResult(election,
                                  effectiveBallots,
                                  effectiveBallots.getCandidatePartyVotes(election.getSeats()),
                                  wasted,
                                  effectiveVoteScore,
                                  votes);
    }

    private Decimal computeEffectiveVoteScore(BallotBox ballots,
                                              JImmutableSet<Candidate> elected)
    {
        var sum = ZERO;
        for (var e : ballots) {
            final var ballot = e.getKey();
            final var count = e.getCount();
            final var increment = count.divide(ballot.size());
            for (Candidate candidate : ballot.getCandidates()) {
                if (elected.contains(candidate)) {
                    sum = sum.plus(increment);
                }
            }
        }
        return sum;
    }
}
