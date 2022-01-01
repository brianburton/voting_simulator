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
    @Getter
    final boolean finished;

    private BasicStvRound(Election election)
    {
        this(election, election.getBallots(), list(), list(), false);
    }

    private BasicStvRound(Election election,
                          BallotBox ballotBox,
                          JImmutableList<CandidateVotes> votes,
                          JImmutableList<Candidate> elected,
                          boolean finished)
    {
        this.election = election;
        this.ballotBox = ballotBox;
        this.votes = votes;
        this.elected = elected;
        this.finished = finished;
    }

    public static BasicStvRound start(Election election)
    {
        return new BasicStvRound(election).advance();
    }

    public BasicStvRound advance()
    {
        assert !finished;
        final var seats = election.getSeats();
        final var quota = election.getQuota();
        final var sortedVotes = computeVotes();
        final var firstPlace = sortedVotes.get(0);
        var newElected = elected;
        var newBallots = ballotBox;
        var finished = false;
        if (sortedVotes.size() <= seats - elected.size()) {
            // we won't be able to improve any so just fill in with whatever we have
            final JImmutableList<Candidate> pluralityWinners = sortedVotes
                .slice(0, seats - newElected.size())
                .transform(CandidateVotes::getCandidate);
            newElected = newElected.insertAllLast(pluralityWinners);
            finished = true;
        } else if (firstPlace.getVotes().isGreaterOrEqualTo(quota)) {
            final var overVote = firstPlace.getVotes().minus(quota);
            final var weight = overVote.dividedBy(firstPlace.getVotes());
            newElected = newElected.insertLast(firstPlace.getCandidate());
            newBallots = newBallots.removeAndTransfer(firstPlace.getCandidate(), weight);
            finished = newElected.size() == seats || newBallots.isEmpty();
        } else {
            final var lastPlace = sortedVotes.get(sortedVotes.size() - 1);
            newBallots = newBallots.remove(lastPlace.getCandidate());
            finished = newBallots.isEmpty();
        }
        if (newBallots.isEmpty() && seats > newElected.size()) {
            System.err.printf("out of ballots with unfilled seats, need %d%n",
                              seats - newElected.size());
        }
        return new BasicStvRound(election, newBallots, sortedVotes, newElected, finished);
    }

    public ElectionResult.RoundResult toElectionResult()
    {
        return new ElectionResult.RoundResult(votes, elected, ballotBox.getExhaustedCount());
    }

    private JImmutableList<CandidateVotes> computeVotes()
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
