package com.burtonzone.model;

import static org.javimmutable.collections.util.JImmutables.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.javimmutable.collections.JImmutableList;
import org.junit.Test;

public class RoundTest
{
    private final Candidate Able = new Candidate("A", 1);
    private final Candidate Baker = new Candidate("B", 2);
    private final Candidate Charlie = new Candidate("C", 3);
    private final Candidate Delta = new Candidate("D", 4);
    private final Candidate Echo = new Candidate("E", 5);

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
        assertEquals(list(cv(Able, 3), cv(Baker, 2)), round.getCounts());

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
        assertEquals(list(cv(Able, 3), cv(Baker, 1), cv(Charlie, 1)), round.getCounts());

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
        assertEquals(list(cv(Able, 4), cv(Baker, 3), cv(Echo, 2)), round.getCounts());

        round = round.next();
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(Baker, 5), cv(Able, 4)), round.getCounts());

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
        assertEquals(list(cv(Able, 11), cv(Delta, 6), cv(Baker, 5), cv(Charlie, 4), cv(Echo, 3)),
                     round.getCounts());

        round = round.next();
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(Able, 11), cv(Charlie, 7), cv(Delta, 6), cv(Baker, 5)),
                     round.getCounts());

        round = round.next();
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(Charlie, 12), cv(Able, 11), cv(Delta, 6)),
                     round.getCounts());

        round = round.next();
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(Charlie, 15), cv(Able, 14)), round.getCounts());

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

    private Round.CandidateVotes cv(Candidate candidate,
                                    int votes)
    {
        return new Round.CandidateVotes(candidate, votes);
    }
}
