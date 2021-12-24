package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import com.burtonzone.parties.Votes;
import java.math.RoundingMode;
import javax.annotation.Nullable;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

public class Round
{
    private final BallotBox ballotBox;
    @Getter
    private final int seats;
    @Getter
    final Decimal totalBallots;
    @Getter
    final Decimal quota;
    private final Votes votes;
    @Getter
    private final JImmutableList<Candidate> elected;
    @Getter
    private final Round prior;

    public Round(BallotBox ballotBox,
                 int seats)
    {
        this.ballotBox = ballotBox;
        this.seats = seats;
        totalBallots = new Decimal(ballotBox.getTotalCount());
        quota = totalBallots.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE)
            .rounded(RoundingMode.DOWN);
        var allCandidates = ballotBox.getCandidates();
        votes = new Votes(allCandidates);
        elected = JImmutables.list();
        prior = null;
    }

    private Round(Votes votes,
                  BallotBox ballotBox,
                  JImmutableList<Candidate> elected,
                  Round prior)
    {
        this.ballotBox = ballotBox;
        this.seats = prior.seats;
        this.totalBallots = prior.totalBallots;
        this.quota = prior.quota;
        this.votes = votes;
        this.elected = elected;
        this.prior = prior;
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
        var newVotes = votes;
        for (JImmutableMap.Entry<Ballot, Integer> entry : ballotBox.ballots()) {
            var ballot = entry.getKey();
            var count = entry.getValue();
            var ballotVotes = ballot.getWeight().times(count);
            newVotes = newVotes.plus(ballot.getFirstChoice(), ballotVotes);
        }
        var sortedVotes = newVotes.getSortedList(ballotBox::compareCandidates);
        var firstPlace = sortedVotes.get(0);
        var newBallots = ballotBox;
        var newElected = elected;
        if (firstPlace.getVotes().isGreaterOrEqualTo(quota)) {
            var overVote = firstPlace.getVotes().minus(quota);
            var weight = overVote.dividedBy(firstPlace.getVotes());
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newVotes = newVotes.without(firstPlace.getCandidate());
            newBallots = newBallots.removeWinner(firstPlace.getCandidate(), weight);
        } else {
            var lastPlace = sortedVotes.get(sortedVotes.size() - 1);
            newBallots = newBallots.removeLoser(lastPlace.getCandidate());
        }
        return new Round(newVotes, newBallots, newElected, this);
    }
}
