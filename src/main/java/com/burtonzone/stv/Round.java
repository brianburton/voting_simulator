package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import com.burtonzone.parties.CandidateVotes;
import com.burtonzone.parties.Votes;
import java.math.RoundingMode;
import javax.annotation.Nullable;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

public class Round
{
    @Getter
    private final Round prior;
    @Getter
    private final BallotBox ballotBox;
    @Getter
    private final int seats;
    @Getter
    final Decimal totalBallots;
    @Getter
    final Decimal quota;
    @Getter
    private final JImmutableList<CandidateVotes> votes;
    @Getter
    private final JImmutableList<Candidate> elected;

    private Round(BallotBox ballotBox,
                  int seats)
    {
        prior = null;
        this.ballotBox = ballotBox;
        this.seats = seats;
        totalBallots = new Decimal(ballotBox.getTotalCount());
        quota = totalBallots.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        votes = JImmutables.list();
        elected = JImmutables.list();
    }

    private Round(Round prior,
                  BallotBox ballotBox,
                  JImmutableList<CandidateVotes> votes,
                  JImmutableList<Candidate> elected)
    {
        this.prior = prior;
        this.ballotBox = ballotBox;
        this.seats = prior.seats;
        totalBallots = new Decimal(ballotBox.getTotalCount());
        quota = totalBallots.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        this.votes = votes;
        this.elected = elected;
    }

    public static RoundBuilder builder()
    {
        return new RoundBuilder();
    }

    public Round getFirstRound()
    {
        Round answer = this;
        while (answer.prior != null) {
            answer = answer.prior;
        }
        return answer;
    }

    public Round run()
    {
        Round answer = this;
        Round next = answer.advance();
        while (next != null) {
            answer = next;
            next = answer.advance();
        }
        return answer;
    }

    @Nullable
    public Round advance()
    {
        if (elected.size() == seats || ballotBox.isEmpty()) {
            return null;
        }
        var newVotes = new Votes();
        for (JImmutableMap.Entry<Ballot, Integer> entry : ballotBox.ballots()) {
            var ballot = entry.getKey();
            var count = entry.getValue();
            var ballotVotes = ballot.getWeight().times(count);
            newVotes = newVotes.plus(ballot.getFirstChoice(), ballotVotes);
        }
        var sortedVotes = newVotes.getSortedList(ballotBox.createCandidateComparator());
        var firstPlace = sortedVotes.get(0);
        var newBallots = ballotBox;
        var newElected = elected;
        if (firstPlace.getVotes().isGreaterOrEqualTo(quota)) {
            var overVote = firstPlace.getVotes().minus(quota);
            var weight = overVote.dividedBy(firstPlace.getVotes());
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newBallots = newBallots.removeAndTransfer(firstPlace.getCandidate(), weight);
        } else if (ballotBox.isExhausted()) {
            // we won't be able to improve any more so just fill in with whatever we have
            var iter = sortedVotes.iterator();
            while (newElected.size() < seats && iter.hasNext()) {
                final Candidate candidate = iter.next().getCandidate();
                newElected = newElected.insertLast(candidate);
                newBallots = newBallots.remove(candidate);
            }
        } else {
            var lastPlace = sortedVotes.get(sortedVotes.size() - 1);
            newBallots = newBallots.remove(lastPlace.getCandidate());
        }
        return new Round(this, newBallots, sortedVotes, newElected);
    }

    public static class RoundBuilder
    {
        private final BallotBox.Builder ballots = BallotBox.builder();
        private int seats = 1;

        public Round build()
        {
            return new Round(ballots.build(), seats);
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
            var ballot = new Ballot(JImmutables.list(candidates), Decimal.ONE);
            return ballot(count, ballot);
        }

        public RoundBuilder ballot(Ballot ballot)
        {
            return ballot(1, ballot);
        }

        public RoundBuilder ballot(int count,
                                   Ballot ballot)
        {
            assert count >= 1;
            ballots.add(ballot, count);
            return this;
        }
    }
}
