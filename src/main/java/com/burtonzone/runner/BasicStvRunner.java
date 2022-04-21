package com.burtonzone.runner;

import static com.burtonzone.common.Decimal.ONE;
import static com.burtonzone.common.Decimal.ZERO;
import static com.burtonzone.election.CandidateVotes.SelectionType.Vote;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.election.BallotBox;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import lombok.Data;
import org.javimmutable.collections.JImmutableList;

public class BasicStvRunner
    implements ElectionRunner
{
    @Override
    public ElectionResult runElection(Election election)
    {
        return runStvElection(election).result;
    }

    public StvResult runStvElection(Election election)
    {
        final var worksheet = new Worksheet(election);
        while (worksheet.remaining > 0) {
            worksheet.nextRound();
        }
        return worksheet.toStvResult();
    }

    @Data
    public static class StvResult
    {
        private final Election election;
        private final JImmutableList<StvRound> stvRounds;
        private final ElectionResult result;
    }

    @Data
    public static class StvRound
    {
        private final BallotBox ballots;
        private final CandidateVotes winner;
        private final CandidateVotes loser;
    }

    private static class Worksheet
    {
        private final Election election;
        private JImmutableList<StvRound> rounds;
        private BallotBox ballots;
        private int remaining;

        private Worksheet(Election election)
        {
            this.election = election;
            rounds = list();
            ballots = election.getBallots();
            remaining = election.getSeats();
        }

        private void nextRound()
        {
            final var votes = computeVotes();
            if (votes.size() < remaining) {
                throw new IllegalStateException("not enough candidates remaining");
            }
            final var winner = votes.get(0);
            final var loser = votes.get(votes.size() - 1);
            if (winner.getVotes().isGreaterOrEqualTo(election.getQuota())) {
                recordQuotaWinner(winner);
            } else if (votes.size() == remaining) {
                for (CandidateVotes vote : votes) {
                    recordRemainderWinner(vote);
                }
            } else {
                recordLoser(loser);
            }
        }

        private JImmutableList<CandidateVotes> computeVotes()
        {
            return ballots.getFirstChoiceCandidateVotes()
                .getSortedList(election.getTieBreaker())
                .transform(cv -> new CandidateVotes(cv, Vote));
        }

        private JImmutableList<CandidateVotes> computeWinnersList()
        {
            return rounds.stream()
                .filter(r -> r.winner != null)
                .map(r -> r.winner)
                .collect(listCollector());
        }

        private void recordQuotaWinner(CandidateVotes winner)
        {
            final var overVote = winner.getVotes().minus(election.getQuota());
            final var transferWeight = overVote.dividedBy(winner.getVotes());
            final var candidate = new CandidateVotes(winner.getCandidate(), election.getQuota(), Vote);
            rounds = rounds.insertLast(new StvRound(ballots, candidate, null));
            ballots = ballots.removeAndTransfer(winner.getCandidate(), transferWeight);
            remaining -= 1;
        }

        private void recordRemainderWinner(CandidateVotes winner)
        {
            rounds = rounds.insertLast(new StvRound(ballots, winner, null));
            ballots = ballots.removeAndTransfer(winner.getCandidate(), ZERO);
            remaining -= 1;
        }

        private void recordLoser(CandidateVotes loser)
        {
            rounds = rounds.insertLast(new StvRound(ballots, null, loser));
            ballots = ballots.removeAndTransfer(loser.getCandidate(), ONE);
        }

        private StvResult toStvResult()
        {
            final var electedVotes = computeWinnersList();
            final var wasted = election.getBallots().countWastedUsingCandidateOnly(electedVotes);
            final var result = new ElectionResult(election,
                                                  election.getBallots(),
                                                  election.getBallots().getCandidatePartyVotes(election.getSeats()),
                                                  wasted,
                                                  electedVotes);
            return new StvResult(election, rounds, result);
        }
    }
}
