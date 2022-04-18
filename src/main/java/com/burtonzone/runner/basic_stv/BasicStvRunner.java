package com.burtonzone.runner.basic_stv;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import org.javimmutable.collections.JImmutableList;

public class BasicStvRunner
    implements ElectionRunner
{
    @Override
    public ElectionResult runElection(Election election)
    {
        final JImmutableList.Builder<ElectionResult.RoundResult> results = listBuilder();
        var round = BasicStvRound.start(election);
        while (!round.isFinished()) {
            round = round.advance();
            results.add(round.toElectionResult());
        }
        final var wasted = election.getBallots().countWastedUsingCandidateOnly(round.getElected());
        return new ElectionResult(election,
                                  results.build(),
                                  election.getBallots(),
                                  election.getBallots().getCandidatePartyVotes(election.getSeats()),
                                  wasted);
    }
}
