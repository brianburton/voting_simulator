package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import java.util.HashMap;
import java.util.Map;
import org.javimmutable.collections.util.JImmutables;

public class RoundBuilder
{
    private final Map<Ballot, Integer> ballots = new HashMap<>();
    private int seats = 1;

    public Round build()
    {
        return new Round(JImmutables.map(ballots), seats);
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
        var current = ballots.get(ballot);
        if (current != null) {
            ballots.put(ballot, current + count);
        } else {
            ballots.put(ballot, 1);
        }
        return this;
    }
}
