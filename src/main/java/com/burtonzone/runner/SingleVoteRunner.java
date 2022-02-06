package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;

public class SingleVoteRunner
    implements ElectionRunner
{
    @Override
    public ElectionResult runElection(Election election)
    {
        var counter = new Counter<Candidate>();
        for (var e : election.getBallots().getFirstChoiceCounts()) {
            final var candidate = e.getKey();
            final var count = e.getCount();
            counter = counter.add(candidate, count);
        }
        var votes = counter
            .getSortedList(election.getTieBreaker())
            .prefix(election.getSeats())
            .transform(CandidateVotes::new);
        var elected = votes.transform(CandidateVotes::getCandidate);
        final var exhausted = Decimal.ZERO;
        final var round = new ElectionResult.RoundResult(votes, elected, exhausted);
        return new ElectionResult(election, list(round));
    }
}
