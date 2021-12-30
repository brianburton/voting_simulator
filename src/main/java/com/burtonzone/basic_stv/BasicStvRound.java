package com.burtonzone.basic_stv;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.election.Ballot;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;

public class BasicStvRound
{
    @Getter
    private final Election election;
    @Getter
    private final BallotBox ballotBox;
    @Getter
    private final JImmutableList<CandidateVotes> votes;
    @Getter
    private final JImmutableList<Candidate> elected;

    public BasicStvRound(Election election)
    {
        this(election, election.getBallots(), list(), list());
    }

    public BasicStvRound(Election election,
                         BallotBox ballotBox,
                         JImmutableList<CandidateVotes> votes,
                         JImmutableList<Candidate> elected)
    {
        this.election = election;
        this.ballotBox = ballotBox;
        this.votes = votes;
        this.elected = elected;
    }

    public BasicStvRound advance()
    {
        assert !isFinal();
        final var seats = election.getSeats();
        final var quota = election.getQuota();
        final var sortedVotes = computeVotes(ballotBox);
        final var firstPlace = sortedVotes.get(0);
        var newElected = elected;
        var newBallots = ballotBox;
        if (firstPlace.getVotes().equals(quota)) {
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newBallots = newBallots.remove(firstPlace.getCandidate());
        } else if (firstPlace.getVotes().isGreaterThan(quota)) {
            var overVote = firstPlace.getVotes().minus(quota);
            var weight = overVote.dividedBy(firstPlace.getVotes());
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newBallots = newBallots.removeAndTransfer(firstPlace.getCandidate(), weight);
        } else if (ballotBox.isExhausted()) {
            // we won't be able to improve any more so just fill in with whatever we have
            final JImmutableList<Candidate> pluralityWinners = sortedVotes.slice(0, seats).transform(CandidateVotes::getCandidate);
            newElected = newElected.insertAllLast(pluralityWinners);
        } else {
            var lastPlace = sortedVotes.get(sortedVotes.size() - 1);
            newBallots = newBallots.remove(lastPlace.getCandidate());
        }
        return new BasicStvRound(election, newBallots, sortedVotes, newElected);
    }

    public ElectionResult.RoundResult toElectionResult()
    {
        return new ElectionResult.RoundResult(votes, elected, ballotBox.getExhaustedCount());
    }

    public boolean isFinal()
    {
        return elected.size() == election.getSeats() || ballotBox.isEmpty();
    }

    private JImmutableList<CandidateVotes> computeVotes(BallotBox ballots)
    {
        var counter = new Counter<Candidate>();
        for (Counter.Entry<Ballot> e : ballotBox.ballots()) {
            var ballot = e.getKey();
            if (!ballot.isEmpty()) {
                counter = counter.add(ballot.getFirstChoice(), e.getCount());
            }
        }
        var sortedVotes = counter
            .getSortedList(election.getTieBreaker())
            .transform(CandidateVotes::new);
        return sortedVotes;
    }
}
