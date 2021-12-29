package com.burtonzone.stv;

import static org.junit.Assert.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import com.burtonzone.parties.Party;
import org.javimmutable.collections.util.JImmutables;
import org.junit.Test;

public class RoundTest
{
    private final Candidate A = new Candidate(Party.CenterLeft, "A");
    private final Candidate B = new Candidate(Party.CenterLeft, "B");
    private final Candidate C = new Candidate(Party.CenterLeft, "C");
    private final Candidate D = new Candidate(Party.CenterLeft, "D");
    private final Candidate E = new Candidate(Party.CenterLeft, "E");

    @Test
    public void sampleWithTieBreaker()
    {
        var election = Round.builder()
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
        var result = election.run();
        var elected = result.getElected();
        assertEquals(JImmutables.list(A, C), elected);
        assertSame(election, result.getFirstRound());
    }

    @Test
    public void simpleTransfer()
    {
        // the two surplus votes from A will boost B to a seat
        final var election = Round.builder()
            .seats(2)
            .ballot(7, A, B)
            .ballot(3, B, A)
            .ballot(4, C)
            .build();

        assertEquals(2, election.getSeats());
        assertEquals(new Decimal(14), election.getTotalBallots());
        assertEquals(new Decimal(5), election.getQuota());

        final var round1 = election.advance();
        assertNotNull(round1);
        assertEquals(JImmutables.list(A), round1.getElected());
        assertSame(election, round1.getPrior());

        final var round2 = round1.advance();
        assertNotNull(round2);
        assertEquals(JImmutables.list(A, B), round2.getElected());
        assertSame(round1, round2.getPrior());

        assertNull(round2.advance());
        assertSame(election, round2.getFirstRound());
    }

    @Test
    public void nonQuotaFinalWinner()
    {
        // in round 2 C has more votes even though no quota
        final var election = Round.builder()
            .seats(2)
            .ballot(50, A)
            .ballot(3, B)
            .ballot(4, C)
            .ballot(2, D)
            .build();

        assertEquals(2, election.getSeats());
        assertEquals(new Decimal(59), election.getTotalBallots());
        assertEquals(new Decimal(20), election.getQuota());

        final var round1 = election.advance();
        assertNotNull(round1);
        assertEquals(JImmutables.list(A), round1.getElected());
        assertSame(election, round1.getPrior());

        final var round2 = round1.advance();
        assertNotNull(round2);
        assertEquals(JImmutables.list(A, C), round2.getElected());
        assertSame(round1, round2.getPrior());

        assertNull(round2.advance());
        assertSame(election, round2.getFirstRound());
    }

    @Test
    public void loserVoteTransfer()
    {
        final var election = Round.builder()
            .seats(2)
            .ballot(2, A, B)  // pushes B to quota in round 2
            .ballot(4, B, A)
            .ballot(5, C, D)
            .ballot(4, D, C)
            .build();

        assertEquals(2, election.getSeats());
        assertEquals(new Decimal(15), election.getTotalBallots());
        assertEquals(new Decimal(6), election.getQuota());

        final var round1 = election.advance();
        assertNotNull(round1);
        assertEquals(JImmutables.list(), round1.getElected());
        assertSame(election, round1.getPrior());

        final var round2 = round1.advance();
        assertNotNull(round2);
        assertEquals(JImmutables.list(B), round2.getElected());
        assertSame(round1, round2.getPrior());

        final var round3 = round2.advance();
        assertNotNull(round3);
        assertEquals(JImmutables.list(B, C), round3.getElected());
        assertSame(round2, round3.getPrior());

        assertNull(round3.advance());
        assertSame(election, round3.getFirstRound());
    }

    @Test
    public void tooFewRankingsLtPluralityWin()
    {
        final var start = Round.builder()
            .seats(1)
            .ballot(21, A, C)
            .ballot(20, B, C)
            .ballot(5, C, A)
            .ballot(5, C, B)
            .ballot(49, D)
            .build();
        final var result = start.run();
        assertEquals(JImmutables.list(D), result.getElected());
    }
}
