package com.burtonzone.old_stv;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.Ballot;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.Votes;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.math.RoundingMode;
import javax.annotation.Nullable;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class OldStvRound
{
    @Getter
    private final OldStvRound prior;
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

    public OldStvRound(Election election)
    {
        this(election.getBallots(), election.getSeats());
    }

    private OldStvRound(BallotBox ballotBox,
                        int seats)
    {
        prior = null;
        this.ballotBox = ballotBox;
        this.seats = seats;
        totalBallots = ballotBox.getTotalCount();
        quota = totalBallots.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        votes = JImmutables.list();
        elected = JImmutables.list();
    }

    private OldStvRound(OldStvRound prior,
                        BallotBox ballotBox,
                        JImmutableList<CandidateVotes> votes,
                        JImmutableList<Candidate> elected,
                        int seats)
    {
        this.prior = prior;
        this.ballotBox = ballotBox;
        this.seats = seats;
        totalBallots = ballotBox.getTotalCount();
        quota = totalBallots.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        this.votes = votes;
        this.elected = elected;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public OldStvRound getFirstRound()
    {
        OldStvRound answer = this;
        while (answer.prior != null) {
            answer = answer.prior;
        }
        return answer;
    }

    public OldStvRound run()
    {
        OldStvRound answer = this;
        OldStvRound next = answer.advance();
        while (next != null) {
            answer = next;
            next = answer.advance();
        }
        return answer;
    }

    @Nullable
    public OldStvRound advance()
    {
        if (seats == 0 || ballotBox.isEmpty()) {
            return null;
        }
        var newVotes = new Votes();
        for (Counter.Entry<Ballot> entry : ballotBox.ballots()) {
            var ballot = entry.getKey();
            if (!ballot.isEmpty()) {
                var ballotVotes = entry.getCount();
                newVotes = newVotes.plus(ballot.getFirstChoice(), ballotVotes);
            }
        }
        var sortedVotes = newVotes.getSortedList(ballotBox.createCandidateComparator());
        var firstPlace = sortedVotes.get(0);
        var newBallots = ballotBox;
        var newElected = elected;
        var newSeats = seats;
        if (firstPlace.getVotes().equals(quota)) {
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newBallots = newBallots.remove(firstPlace.getCandidate());
            newSeats -= 1;
        } else if (firstPlace.getVotes().isGreaterThan(quota)) {
            var overVote = firstPlace.getVotes().minus(quota);
            var weight = overVote.dividedBy(firstPlace.getVotes());
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newBallots = newBallots.removeAndTransfer(firstPlace.getCandidate(), weight);
            newSeats -= 1;
        } else if (ballotBox.isExhausted()) {
            // we won't be able to improve any more so just fill in with whatever we have
            final JImmutableList<Candidate> pluralityWinners = sortedVotes.slice(0, seats).transform(CandidateVotes::getCandidate);
            newElected = newElected.insertAllLast(pluralityWinners);
            newSeats = 0;
        } else {
            var lastPlace = sortedVotes.get(sortedVotes.size() - 1);
            newBallots = newBallots.remove(lastPlace.getCandidate());
        }
        return new OldStvRound(this, newBallots, sortedVotes, newElected, newSeats);
    }

    public ElectionResult.RoundResult toElectionResult()
    {
        return new ElectionResult.RoundResult(votes, elected, ballotBox.getExhaustedCount());
    }

    public static class Builder
    {
        private final Election.Builder election = Election.builder();

        public OldStvRound build()
        {
            return new OldStvRound(election.build());
        }

        @CanIgnoreReturnValue
        public Builder seats(int val)
        {
            election.seats(val);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder ballot(Candidate... candidates)
        {
            return ballot(1, candidates);
        }

        @CanIgnoreReturnValue
        public Builder ballot(int count,
                              Candidate... candidates)
        {
            var ballot = new Ballot(JImmutables.list(candidates));
            return ballot(count, ballot);
        }

        @CanIgnoreReturnValue
        public Builder ballot(int count,
                              Ballot ballot)
        {
            assert count >= 1;
            election.ballot(count, ballot);
            return this;
        }
    }
}
