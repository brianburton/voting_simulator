package com.burtonzone.runner.basic_stv;

import static org.junit.Assert.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import org.javimmutable.collections.util.JImmutables;
import org.junit.Test;

public class BasicStvRoundTest
{
    private final Candidate A = new Candidate(Party.CenterLeft, "A");
    private final Candidate B = new Candidate(Party.CenterLeft, "B");
    private final Candidate C = new Candidate(Party.CenterLeft, "C");
    private final Candidate D = new Candidate(Party.CenterLeft, "D");
    private final Candidate E = new Candidate(Party.CenterLeft, "E");
    private final ElectionRunner runner = new BasicStvRunner();

    @Test
    public void sampleWithTieBreaker()
    {
        var election = Election.builder()
            .seats(2)
            .ballot(A, E, B, D, C)
            .ballot(A, D, B, C, E)
            .ballot(E, C, B, D, A)
            .ballot(B, C, E, D, A)
            .ballot(B, A, E, D, C)
            .ballot(A, C, E, D, B)
            .ballot(C, E, A, B, D)
            .ballot(E, C, B, A, D)
            .ballot(C, A, B, E, D)
            .ballot(D, A, C, E, B)
            .build();
        var result = runner.runElection(election);
        var elected = result.getElected();
        assertEquals(JImmutables.list(A, C), elected);
    }

    @Test
    public void simpleTransfer()
    {
        // the two surplus votes from A will boost B to a seat
        final var election = Election.builder()
            .seats(2)
            .ballot(7, A, B)
            .ballot(3, B, A)
            .ballot(4, C)
            .build();

        assertEquals(2, election.getSeats());
        assertEquals(new Decimal(14), election.getTotalVotes());
        assertEquals(new Decimal(5), election.getQuota());

        final var round1 = BasicStvRound.start(election);
        assertNotNull(round1);
        assertEquals(JImmutables.list(A), round1.getElected());

        final var round2 = round1.advance();
        assertTrue(round2.isFinished());
        assertEquals(JImmutables.list(A, B), round2.getElected());
    }

    @Test
    public void nonQuotaFinalWinner()
    {
        // in round 2 C has more votes even though no quota
        final var election = Election.builder()
            .seats(2)
            .ballot(50, A)
            .ballot(3, B)
            .ballot(4, C)
            .ballot(2, D)
            .build();

        assertEquals(2, election.getSeats());
        assertEquals(new Decimal(59), election.getTotalVotes());
        assertEquals(new Decimal(20), election.getQuota());

        final var round1 = BasicStvRound.start(election);
        assertNotNull(round1);
        assertEquals(JImmutables.list(A), round1.getElected());

        final var round2 = round1.advance();
        assertFalse(round2.isFinished());
        assertEquals(JImmutables.list(A), round2.getElected());

        final var round3 = round2.advance();
        assertFalse(round3.isFinished());
        assertEquals(JImmutables.list(A), round3.getElected());

        final var round4 = round3.advance();
        assertTrue(round4.isFinished());
        assertEquals(JImmutables.list(A, C), round4.getElected());
    }

    @Test
    public void loserVoteTransfer()
    {
        final var election = Election.builder()
            .seats(2)
            .ballot(2, A, B)  // pushes B to quota in round 2
            .ballot(4, B, A)
            .ballot(5, C, D)
            .ballot(4, D, C)
            .build();

        assertEquals(2, election.getSeats());
        assertEquals(new Decimal(15), election.getTotalVotes());
        assertEquals(new Decimal(6), election.getQuota());

        final var round1 = BasicStvRound.start(election);
        assertFalse(round1.isFinished());
        assertEquals(JImmutables.list(), round1.getElected());

        final var round2 = round1.advance();
        assertFalse(round2.isFinished());
        assertEquals(JImmutables.list(B), round2.getElected());

        final var round3 = round2.advance();
        assertFalse(round3.isFinished());
        assertEquals(JImmutables.list(B), round3.getElected());

        final var round4 = round3.advance();
        assertTrue(round4.isFinished());
        assertEquals(JImmutables.list(B, C), round4.getElected());
    }

    @Test
    public void tooFewRankingsLtPluralityWin()
    {
        final var election = Election.builder()
            .seats(1)
            .ballot(21, A, C)
            .ballot(20, B, C)
            .ballot(5, C, A)
            .ballot(5, C, B)
            .ballot(49, D)
            .build();
        final var result = runner.runElection(election);
        assertEquals(JImmutables.list(D), result.getElected());
    }

    @Test
    public void multiRoundTest()
    {
        // sample election from wikipedia https://en.wikipedia.org/wiki/Single_transferable_vote
        final var election = Election.builder()
            .seats(3)
            .ballot(5, A, B)
            .ballot(3, B, A)
            .ballot(8, C, D)
            .ballot(4, C, E)
            .ballot(1, D, B)
            .ballot(2, E, B)
            .build();
        final var result = runner.runElection(election);
        assertEquals(JImmutables.list(C, A, D), result.getElected());
    }
}
