package com.burtonzone.runner;

import static com.burtonzone.common.Decimal.ONE;
import static com.burtonzone.common.Decimal.ZERO;
import static com.burtonzone.election.CandidateVotes.SelectionType.Vote;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import lombok.Data;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;

public class StvRunner
    implements ElectionRunner
{
    @Override
    public Result runElection(Election election)
    {
        final var worksheet = new Worksheet(election);
        while (worksheet.remaining > 0) {
            worksheet.nextRound();
        }
        return worksheet.toStvResult();
    }

    public static class Result
        extends ElectionResult
    {
        @Getter
        private final JImmutableList<Round> rounds;

        public Result(Election election,
                      BallotBox effectiveBallots,
                      Counter<Party> partyVotes,
                      Decimal wasted,
                      Decimal effectiveVoteScore,
                      JImmutableList<CandidateVotes> electedVotes,
                      JImmutableList<Round> rounds)
        {
            super(election, effectiveBallots, partyVotes, wasted, effectiveVoteScore, electedVotes);
            this.rounds = rounds;
        }
    }

    @Data
    public static class Round
    {
        private final BallotBox ballots;
        private final CandidateVotes winner;
        private final CandidateVotes loser;
    }

    private static class Worksheet
    {
        private final Election election;
        private JImmutableList<Round> rounds;
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
            if (winner.getVotes().isGreaterOrEqualTo(election.getQuota())) {
                recordQuotaWinner(winner);
            } else if (votes.size() == remaining) {
                recordRemainderWinners(votes);
            } else {
                final var loser = votes.get(votes.size() - 1);
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
            rounds = rounds.insertLast(new Round(ballots, candidate, null));
            ballots = ballots.removeAndTransfer(winner.getCandidate(), transferWeight);
            remaining -= 1;
        }

        private void recordRemainderWinners(JImmutableList<CandidateVotes> winners)
        {
            for (CandidateVotes winner : winners) {
                rounds = rounds.insertLast(new Round(ballots, winner, null));
                ballots = ballots.removeAndTransfer(winner.getCandidate(), ZERO);
                remaining -= 1;
            }
        }

        private void recordLoser(CandidateVotes loser)
        {
            rounds = rounds.insertLast(new Round(ballots, null, loser));
            ballots = ballots.removeAndTransfer(loser.getCandidate(), ONE);
        }

        private Decimal computeEffectiveVoteScore(JImmutableSet<Candidate> elected)
        {
            var sum = ZERO;
            for (var e : election.getBallots()) {
                final var ballot = e.getKey();
                final var count = e.getCount();
                final var drop = ONE.dividedBy(new Decimal(ballot.size()));
                var fraction = ONE;
                for (Candidate candidate : ballot.getCandidates()) {
                    if (elected.contains(candidate)) {
                        sum = sum.plus(count.times(fraction));
                        break;
                    }
                    fraction = fraction.minus(drop);
                }
            }
            return sum;
        }

        private Result toStvResult()
        {
            final var electedVotes = computeWinnersList();
            final var electedCandidates = CandidateVotes.toCandidateSet(electedVotes);
            final var partyVotes = election.getBallots().getCandidatePartyVotes(election.getSeats());
            final var wasted = election.getBallots().countWastedUsingCandidateOnly(electedVotes);
            final var effectiveVoteScore = computeEffectiveVoteScore(electedCandidates);
            return new Result(election,
                              election.getBallots(),
                              partyVotes,
                              wasted,
                              effectiveVoteScore,
                              electedVotes,
                              rounds);
        }
    }
}
