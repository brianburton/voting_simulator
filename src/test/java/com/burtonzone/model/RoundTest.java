package com.burtonzone.model;

import static org.javimmutable.collections.util.JImmutables.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.javimmutable.collections.JImmutableList;
import org.junit.Test;

public class RoundTest
{
    private final Candidate Able = new Candidate("Able");
    private final Candidate Baker = new Candidate("Baker");
    private final Candidate Charlie = new Candidate("Charlie");
    private final Candidate Delta = new Candidate("Delta");
    private final Candidate Echo = new Candidate("Echo");

    @Test
    public void simpleMajority()
    {
        var ballots = ballots()
            .insert(ballot(Able, Baker, Charlie))
            .insert(ballot(Baker, Charlie))
            .insert(ballot(Able, Charlie))
            .insert(ballot(Baker, Able))
            .insert(ballot(Able, Baker));
        var round = new Round(ballots);
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(3, Able), cv(2, Baker)), round.getCounts());

        assertEquals(round, round.solve());
    }

    @Test
    public void simpleMajority2()
    {
        var ballots = ballots()
            .insert(ballot(Able, Baker, Charlie))
            .insert(ballot(Baker, Charlie))
            .insert(ballot(Able, Charlie))
            .insert(ballot(Charlie, Delta))
            .insert(ballot(Able, Baker));
        var round = new Round(ballots);
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(3, Able), cv(1, Baker, Charlie)), round.getCounts());

        assertEquals(round, round.solve());
    }

    @Test
    public void twoRounds()
    {
        var ballots = ballots()
            .insert(ballot(Able, Delta))
            .insert(ballot(Baker, Charlie))
            .insert(ballot(Able, Delta))
            .insert(ballot(Baker, Charlie))
            .insert(ballot(Able, Delta))
            .insert(ballot(Baker, Delta))
            .insert(ballot(Able, Echo))
            .insert(ballot(Echo, Baker))
            .insert(ballot(Echo, Baker));
        var round = new Round(ballots);
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(4, Able), cv(3, Baker), cv(2, Echo)), round.getCounts());

        round = round.next();
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(5, Baker), cv(4, Able)), round.getCounts());

        assertEquals(round, new Round(ballots).solve());
    }

    @Test
    public void threeRounds()
    {
        var ballots = ballots()
            .insertAll(repeat(5, Able, Delta, Echo))
            .insertAll(repeat(6, Able, Echo, Baker))
            .insertAll(repeat(5, Baker, Charlie, Echo))
            .insertAll(repeat(3, Delta, Charlie, Echo))
            .insertAll(repeat(3, Echo, Charlie, Delta))
            .insertAll(repeat(3, Delta, Able, Baker))
            .insertAll(repeat(4, Charlie, Echo, Delta));
        var round = new Round(ballots);
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(11, Able), cv(6, Delta), cv(5, Baker), cv(4, Charlie), cv(3, Echo)),
                     round.getCounts());

        round = round.next();
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(11, Able), cv(7, Charlie), cv(6, Delta), cv(5, Baker)),
                     round.getCounts());

        round = round.next();
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(12, Charlie), cv(11, Able), cv(6, Delta)),
                     round.getCounts());

        round = round.next();
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(15, Charlie), cv(14, Able)), round.getCounts());

        assertEquals(round, new Round(ballots).solve());
    }

    private JImmutableList<Ballot> repeat(int count,
                                          Candidate... candidates)
    {
        var answer = ballots();
        for (int i = 0; i < count; ++i) {
            answer = answer.insert(ballot(candidates));
        }
        return answer;
    }

    private JImmutableList<Ballot> ballots()
    {
        return list();
    }

    private Ballot ballot(Candidate... candidates)
    {
        return new Ballot(list(candidates));
    }

    private Round round(Ballot... ballots)
    {
        return new Round(Arrays.asList(ballots));
    }

    private Round.CandidateVotes cv(int votes,
                                    Candidate... candidates)
    {
        return new Round.CandidateVotes(votes, list(candidates));
    }
}
