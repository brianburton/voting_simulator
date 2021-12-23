package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.CandidateVotes;
import com.burtonzone.parties.Votes;
import javax.annotation.Nullable;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

public class Round
{
    private final JImmutableMap<Ballot, Integer> ballots;
    @Getter
    private final int seats;
    @Getter
    final Decimal totalBallots;
    @Getter
    final Decimal quota;
    private final Votes votes;
    @Getter
    private final JImmutableList<CandidateVotes> elected;
    @Getter
    private final Round prior;

    public Round(JImmutableMap<Ballot, Integer> ballots,
                 int seats)
    {
        this.ballots = ballots;
        this.seats = seats;
        totalBallots = new Decimal(ballots.values().stream().mapToInt(i -> i).sum());
        quota = totalBallots.dividedBy(new Decimal(seats + 1))
            .plus(Decimal.ONE);
        var allCandidates = ballots
            .keys().stream()
            .flatMap(b -> b.getChoices().stream());
        votes = new Votes(allCandidates);
        elected = JImmutables.list();
        prior = null;
    }

    private Round(Votes votes,
                  JImmutableMap<Ballot, Integer> ballots,
                  JImmutableList<CandidateVotes> elected,
                  Round prior)
    {
        this.ballots = ballots;
        this.seats = prior.seats;
        this.totalBallots = prior.totalBallots;
        this.quota = prior.quota;
        this.votes = votes;
        this.elected = elected;
        this.prior = prior;
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
        if (elected.size() == seats || ballots.size() == 0) {
            return null;
        }
        var newVotes = votes;
        for (JImmutableMap.Entry<Ballot, Integer> entry : ballots) {
            var ballot = entry.getKey();
            var count = entry.getValue();
            var ballotVotes = ballot.getWeight().times(count);
            newVotes = newVotes.plus(ballot.getFirstChoice(), ballotVotes);
        }
        var sortedVotes = newVotes.getSortedList();
        var firstPlace = sortedVotes.get(0);
        var newBallots = ballots;
        var newElected = elected;
        if (firstPlace.getVotes().isGreaterOrEqualTo(quota)) {
            var overVote = firstPlace.getVotes().minus(quota);
            var weight = overVote.dividedBy(firstPlace.getVotes());
            newElected = newElected.insertLast(firstPlace.withVotes(quota));
            for (JImmutableMap.Entry<Ballot, Integer> e : newBallots) {
                var ballot = e.getKey();
                var count = e.getValue();
                var newBallot = ballot.without(firstPlace.getCandidate());
                if (newBallot != ballot) {
                    newBallots = newBallots.delete(ballot);
                    if (ballot.getFirstChoice().equals(firstPlace.getCandidate())) {
                        newBallot = newBallot.weighted(weight);
                        if (newBallot.getWeight().isGreaterThan(Decimal.ZERO)) {
                            newBallots = newBallots.assign(newBallot, count);
                        }
                    }
                }
            }
        } else {
            var lastPlace = sortedVotes.get(sortedVotes.size() - 1);
            for (JImmutableMap.Entry<Ballot, Integer> e : newBallots) {
                var ballot = e.getKey();
                var count = e.getValue();
                var newBallot = ballot.without(lastPlace.getCandidate());
                if (newBallot != ballot) {
                    newBallots = newBallots
                        .delete(ballot)
                        .assign(newBallot, count);
                }
            }
        }
        return new Round(newVotes, newBallots, newElected, this);
    }
}
