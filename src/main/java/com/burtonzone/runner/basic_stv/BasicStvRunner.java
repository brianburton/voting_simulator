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
        do {
            results.add(round.toElectionResult());
            round = round.advance();
        } while (!round.isFinished());
        results.add(round.toElectionResult());
        return new ElectionResult(election, results.build());
    }
}
