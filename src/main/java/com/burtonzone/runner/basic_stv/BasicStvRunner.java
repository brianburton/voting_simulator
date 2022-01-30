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
        BasicStvRound round = BasicStvRound.start(election);
        while (true) {
            results.add(round.toElectionResult());
            if (round.isFinished()) {
                break;
            }
            round = round.advance();
        }
        return new ElectionResult(election, results.build());
    }
}
