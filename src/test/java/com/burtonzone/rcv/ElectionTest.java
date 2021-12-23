package com.burtonzone.rcv;

import static org.javimmutable.collections.util.JImmutables.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ElectionTest
{
    private final Candidate Able = new Candidate("Able");
    private final Candidate Baker = new Candidate("Baker");
    private final Candidate Charlie = new Candidate("Charlie");
    private final Candidate Delta = new Candidate("Delta");
    private final Candidate Echo = new Candidate("Echo");
    private final Candidate Foxtrot = new Candidate("Foxtrot");

    @Test
    public void basisPoints()
    {
        assertEquals(100, Election.basisPoints(1, 100));
        assertEquals(25, Election.basisPoints(1, 400));
        assertEquals(10, Election.basisPoints(1, 1000));
        assertEquals(1, Election.basisPoints(1, 10000));
    }

    @Test
    public void minBasisPoints()
    {
        assertEquals(5001, Election.minBasisPointsForNumOffices(1));
        assertEquals(3334, Election.minBasisPointsForNumOffices(2));
        assertEquals(2501, Election.minBasisPointsForNumOffices(3));
        assertEquals(2001, Election.minBasisPointsForNumOffices(4));
        assertEquals(1667, Election.minBasisPointsForNumOffices(5));
    }

    @Test
    public void oneOfficeThreeRounds()
    {
        final var builder = Round.builder()
            .insertRepeat(5, new Ballot(Able, Delta, Echo))
            .insertRepeat(6, new Ballot(Able, Echo, Baker))
            .insertRepeat(5, new Ballot(Baker, Charlie, Echo))
            .insertRepeat(3, new Ballot(Delta, Charlie, Echo))
            .insertRepeat(3, new Ballot(Echo, Charlie, Delta))
            .insertRepeat(3, new Ballot(Delta, Able, Baker))
            .insertRepeat(4, new Ballot(Charlie, Echo, Delta));
        final var election = new Election(1);
        final var result = election.run(builder.build());
        assertEquals(list(cv(15, Charlie), cv(14, Able)), result.getCounts());
        assertEquals(list(cv(15, Charlie)), election.getElected(result));
    }

    @Test
    public void twoOffices()
    {
        final var builder = Round.builder()
            .insertRepeat(5, new Ballot(Able, Delta, Echo))
            .insertRepeat(6, new Ballot(Able, Echo, Baker))
            .insertRepeat(5, new Ballot(Baker, Charlie, Echo))
            .insertRepeat(3, new Ballot(Delta, Charlie, Echo))
            .insertRepeat(3, new Ballot(Echo, Charlie, Delta))
            .insertRepeat(3, new Ballot(Delta, Able, Baker))
            .insertRepeat(4, new Ballot(Charlie, Echo, Delta));
        final var election = new Election(2);
        final var result = election.run(builder.build());
        assertEquals(list(cv(12, Charlie), cv(11, Able), cv(6, Delta)), result.getCounts());
        assertEquals(list(cv(12, Charlie), cv(11, Able)), election.getElected(result));
    }

    @Test
    public void threeOffices()
    {
        final var builder = Round.builder()
            .insertRepeat(5, new Ballot(Able, Delta, Echo))
            .insertRepeat(6, new Ballot(Able, Echo, Baker))
            .insertRepeat(5, new Ballot(Baker, Charlie, Echo))
            .insertRepeat(6, new Ballot(Delta, Charlie, Echo))
            .insertRepeat(3, new Ballot(Echo, Charlie, Delta))
            .insertRepeat(3, new Ballot(Delta, Able, Baker))
            .insertRepeat(4, new Ballot(Charlie, Echo, Delta));
        final var election = new Election(3);
        final var result = election.run(builder.build());
        assertEquals(list(cv(12, Charlie), cv(11, Able), cv(9, Delta)), result.getCounts());
        assertEquals(list(cv(12, Charlie), cv(11, Able), cv(9, Delta)), election.getElected(result));
    }

    @Test
    public void insufficientVotes()
    {
        final var builder = Round.builder()
            .insertRepeat(1, new Ballot(Able))
            .insertRepeat(2, new Ballot(Baker))
            .insertRepeat(1, new Ballot(Charlie))
            .insertRepeat(1, new Ballot(Delta))
            .insertRepeat(2, new Ballot(Echo))
            .insertRepeat(1, new Ballot(Foxtrot));
        final var election = new Election(3);
        final var result = election.run(builder.build());
        assertEquals(list(cv(2, Baker, Echo), cv(1, Able, Charlie, Delta, Foxtrot)), result.getCounts());
        assertEquals(list(), election.getElected(result));
    }

    private Round.CandidateVotes cv(int votes,
                                    Candidate... candidates)
    {
        return new Round.CandidateVotes(votes, list(candidates));
    }
}
