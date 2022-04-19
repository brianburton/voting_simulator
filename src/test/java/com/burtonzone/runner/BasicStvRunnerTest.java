package com.burtonzone.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.burtonzone.common.Decimal;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.LinearPosition;
import com.burtonzone.election.Party;
import org.javimmutable.collections.util.JImmutables;
import org.junit.Test;

public class BasicStvRunnerTest
{
    private final Party P = new Party("P", "P", new LinearPosition(10));
    private final Candidate A = new Candidate(P, "A");
    private final Candidate B = new Candidate(P, "B");
    private final Candidate C = new Candidate(P, "C");
    private final Candidate D = new Candidate(P, "D");
    private final Candidate E = new Candidate(P, "E");
    private final BasicStvRunner runner = new BasicStvRunner();

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

        final BasicStvRunner.StvResult result = runner.runStvElection(election);

        var elected = result.getResult().getElected();
        assertEquals(JImmutables.list(A, B), elected);

        final var rounds = result.getStvRounds();
        assertEquals(2, rounds.size());

        final var round1 = rounds.get(0);
        assertNotNull(round1);
        assertEquals(cv(A, "5"), round1.getWinner());

        final var round2 = rounds.get(1);
        assertEquals(cv(B, "5"), round2.getWinner());

        assertEquals(new Decimal("4"), result.getResult().getWasted());
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

        final BasicStvRunner.StvResult result = runner.runStvElection(election);

        var elected = result.getResult().getElected();
        assertEquals(JImmutables.list(A, C), elected);

        final var rounds = result.getStvRounds();
        assertEquals(4, rounds.size());

        final var round1 = rounds.get(0);
        assertNotNull(round1);
        assertEquals(cv(A, "20"), round1.getWinner());

        final var round2 = rounds.get(1);
        assertEquals(cv(D, "2"), round2.getLoser());

        final var round3 = rounds.get(2);
        assertEquals(cv(B, "3"), round3.getLoser());

        final var round4 = rounds.get(3);
        assertEquals(cv(C, "4"), round4.getWinner());

        assertEquals(new Decimal("5"), result.getResult().getWasted());
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

        final BasicStvRunner.StvResult result = runner.runStvElection(election);

        var elected = result.getResult().getElected();
        assertEquals(JImmutables.list(B, C), elected);

        final var rounds = result.getStvRounds();
        assertEquals(4, rounds.size());

        final var round1 = rounds.get(0);
        assertEquals(cv(A, "2"), round1.getLoser());

        final var round2 = rounds.get(1);
        assertEquals(cv(B, "6"), round2.getWinner());

        final var round3 = rounds.get(2);
        assertEquals(cv(D, "4"), round3.getLoser());

        final var round4 = rounds.get(3);
        assertEquals(cv(C, "6"), round4.getWinner());

        assertEquals(new Decimal("0"), result.getResult().getWasted());
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

        assertEquals(1, election.getSeats());
        assertEquals(new Decimal(100), election.getTotalVotes());
        assertEquals(new Decimal(51), election.getQuota());

        final BasicStvRunner.StvResult result = runner.runStvElection(election);

        var elected = result.getResult().getElected();
        assertEquals(JImmutables.list(D), elected);

        final var rounds = result.getStvRounds();
        assertEquals(4, rounds.size());

        final var round1 = rounds.get(0);
        assertEquals(cv(C, "10"), round1.getLoser());

        final var round2 = rounds.get(1);
        assertEquals(cv(B, "25"), round2.getLoser());

        final var round3 = rounds.get(2);
        assertEquals(cv(A, "26"), round3.getLoser());

        final var round4 = rounds.get(3);
        assertEquals(cv(D, "49"), round4.getWinner());

        assertEquals(new Decimal("51"), result.getResult().getWasted());
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

        assertEquals(3, election.getSeats());
        assertEquals(new Decimal(23), election.getTotalVotes());
        assertEquals(new Decimal(6), election.getQuota());

        final BasicStvRunner.StvResult result = runner.runStvElection(election);

        var elected = result.getResult().getElected();
        assertEquals(JImmutables.list(C, A, D), elected);

        final var rounds = result.getStvRounds();
        assertEquals(5, rounds.size());

        final var round1 = rounds.get(0);
        assertEquals(cv(C, "6"), round1.getWinner());

        final var round2 = rounds.get(1);
        assertEquals(cv(B, "3"), round2.getLoser());

        final var round3 = rounds.get(2);
        assertEquals(cv(A, "6"), round3.getWinner());

        final var round4 = rounds.get(3);
        assertEquals(cv(E, "4"), round4.getLoser());

        final var round5 = rounds.get(4);
        assertEquals(cv(D, "5"), round5.getWinner());

        assertEquals(new Decimal("2"), result.getResult().getWasted());
    }

    private static CandidateVotes cv(Candidate candidate,
                                     String votes)
    {
        return new CandidateVotes(candidate, new Decimal(votes), CandidateVotes.SelectionType.Vote);
    }
}
