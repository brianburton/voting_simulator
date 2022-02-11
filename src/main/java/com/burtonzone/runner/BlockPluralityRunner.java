package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;

/**
 * NOT a PR system at all.  In fact this is the opposite since it produces false majorities.
 * It can allow a majority of voters to capture all seats in an election or a plurality to
 * win a majority of the seats.
 *
 * Included here simply to demonstrate a bad system.
 *
 * Each voter gets one vote per seat and candidates with the highest vote counts are elected.
 */
public class BlockPluralityRunner
    implements ElectionRunner

{
    @Override
    public ElectionResult runElection(Election election)
    {
        final var effectiveBallots = election.getBallots().toPrefixBallots(election.getSeats());
        var counter = new Counter<Candidate>();
        for (var ballot : effectiveBallots) {
            for (Candidate candidate : ballot.getKey()) {
                var count = ballot.getCount();
                counter = counter.add(candidate, count);
            }
        }
        var votes = counter
            .getSortedList(election.getTieBreaker())
            .prefix(election.getSeats())
            .transform(CandidateVotes::new);
        var elected = votes.transform(CandidateVotes::getCandidate);
        final var exhausted = Decimal.ZERO;
        final var round = new ElectionResult.RoundResult(votes, elected, exhausted);
        return new ElectionResult(election, list(round), effectiveBallots);
    }
}
