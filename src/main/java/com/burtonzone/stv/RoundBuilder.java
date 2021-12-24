package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import org.javimmutable.collections.util.JImmutables;

public class RoundBuilder
{
    private final BallotBox.Builder ballots = BallotBox.builder();
    private int seats = 1;

    public Round build()
    {
        return new Round(ballots.build(), seats);
    }

    public RoundBuilder seats(int val)
    {
        seats = val;
        assert seats > 0;
        return this;
    }

    public RoundBuilder ballot(Candidate... candidates)
    {
        return ballot(1, candidates);
    }

    public RoundBuilder ballot(int count,
                               Candidate... candidates)
    {
        assert count >= 1;
        var ballot = new Ballot(JImmutables.list(candidates), Decimal.ONE);
        ballots.add(ballot, count);
        return this;
    }
}
