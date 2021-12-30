package com.burtonzone.old_stv;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import org.javimmutable.collections.JImmutableList;

public class OldStvRunner
    implements ElectionRunner
{
    @Override
    public ElectionResult runElection(Election election)
    {
        final JImmutableList.Builder<ElectionResult.RoundResult> results = listBuilder();
        var round = new OldStvRound(election);
        do {
            results.add(round.toElectionResult());
            round = round.advance();
        } while (round != null);
        return new ElectionResult(election, results.build());
    }
}
