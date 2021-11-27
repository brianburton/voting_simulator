package com.burtonzone.model;

import static org.javimmutable.collections.util.JImmutables.*;
import static org.junit.Assert.*;

import java.util.NoSuchElementException;
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
        var round = Round.builder()
            .insert(new Ballot(Able, Baker, Charlie))
            .insert(new Ballot(Baker, Charlie))
            .insert(new Ballot(Able, Charlie))
            .insert(new Ballot(Baker, Able))
            .insert(new Ballot(Able, Baker))
            .build();

        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(3, Able), cv(2, Baker)), round.getCounts());

        assertEquals(round, round.solve());
    }

    @Test
    public void simpleMajority2()
    {
        var round = Round.builder()
            .insert(new Ballot(Able, Baker, Charlie))
            .insert(new Ballot(Baker, Charlie))
            .insert(new Ballot(Able, Charlie))
            .insert(new Ballot(Charlie, Delta))
            .insert(new Ballot(Able, Baker))
            .build();
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(3, Able), cv(1, Baker, Charlie)), round.getCounts());

        assertEquals(round, round.solve());
    }

    @Test
    public void twoRounds()
    {
        var builder = Round.builder()
            .insert(new Ballot(Able, Delta))
            .insert(new Ballot(Baker, Charlie))
            .insert(new Ballot(Able, Delta))
            .insert(new Ballot(Baker, Charlie))
            .insert(new Ballot(Able, Delta))
            .insert(new Ballot(Baker, Delta))
            .insert(new Ballot(Able, Echo))
            .insert(new Ballot(Echo, Baker))
            .insert(new Ballot(Echo, Baker));
        var round = builder.build();
        assertEquals(false, round.isMajority());
        assertEquals(true, round.hasNext());
        assertEquals(list(cv(4, Able), cv(3, Baker), cv(2, Echo)), round.getCounts());

        round = round.next();
        assertEquals(true, round.isMajority());
        assertEquals(false, round.hasNext());
        assertEquals(list(cv(5, Baker), cv(4, Able)), round.getCounts());

        assertEquals(round, builder.build().solve());
    }

    @Test
    public void threeRounds()
    {
        final var builder = Round.builder()
            .insertRepeat(5, new Ballot(Able, Delta, Echo))
            .insertRepeat(6, new Ballot(Able, Echo, Baker))
            .insertRepeat(5, new Ballot(Baker, Charlie, Echo))
            .insertRepeat(3, new Ballot(Delta, Charlie, Echo))
            .insertRepeat(3, new Ballot(Echo, Charlie, Delta))
            .insertRepeat(3, new Ballot(Delta, Able, Baker))
            .insertRepeat(4, new Ballot(Charlie, Echo, Delta));
        final var round1 = builder.build();
        assertEquals(false, round1.hasPrior());
        assertThrows(NoSuchElementException.class, round1::prior);
        assertEquals(false, round1.isMajority());
        assertEquals(true, round1.hasNext());
        assertEquals(list(cv(11, Able), cv(6, Delta), cv(5, Baker), cv(4, Charlie), cv(3, Echo)),
                     round1.getCounts());

        final var round2 = round1.next();
        assertEquals(true, round2.hasPrior());
        assertSame(round1, round2.prior());
        assertEquals(false, round2.isMajority());
        assertEquals(true, round2.hasNext());
        assertEquals(list(cv(11, Able), cv(7, Charlie), cv(6, Delta), cv(5, Baker)),
                     round2.getCounts());

        final var round3 = round2.next();
        assertEquals(true, round3.hasPrior());
        assertSame(round2, round3.prior());
        assertEquals(false, round3.isMajority());
        assertEquals(true, round3.hasNext());
        assertEquals(list(cv(12, Charlie), cv(11, Able), cv(6, Delta)),
                     round3.getCounts());

        final var round4 = round3.next();
        assertEquals(true, round4.hasPrior());
        assertSame(round3, round4.prior());
        assertEquals(true, round4.isMajority());
        assertEquals(false, round4.hasNext());
        assertEquals(list(cv(15, Charlie), cv(14, Able)), round4.getCounts());
        assertThrows(NoSuchElementException.class, round4::next);

        assertEquals(round4, round1.solve());
        assertEquals(list(round1, round2, round3, round4), round4.toList());
    }

    private Round.CandidateVotes cv(int votes,
                                    Candidate... candidates)
    {
        return new Round.CandidateVotes(votes, list(candidates));
    }
}
