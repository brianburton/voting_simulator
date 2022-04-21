package com.burtonzone.election;

import static com.burtonzone.common.Decimal.ZERO;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class ElectionResult
{
    private final Election election;
    private final BallotBox effectiveBallots;
    private final Counter<Party> partyVotes;
    private final Decimal wasted;
    private final JImmutableList<CandidateVotes> electedVotes;

    public ElectionResult(Election election,
                          BallotBox effectiveBallots,
                          Counter<Party> partyVotes,
                          Decimal wasted,
                          JImmutableList<CandidateVotes> electedVotes)
    {
        this.election = election;
        this.effectiveBallots = effectiveBallots;
        this.partyVotes = partyVotes;
        this.wasted = wasted;
        this.electedVotes = electedVotes;
    }

    public Election getElection()
    {
        return election;
    }

    public BallotBox getEffectiveBallots()
    {
        return effectiveBallots;
    }

    public JImmutableList<Candidate> getElected()
    {
        return electedVotes.transform(CandidateVotes::getCandidate);
    }

    public JImmutableList<CandidateVotes> getVotes()
    {
        return electedVotes;
    }

    public int getElectedCount()
    {
        return electedVotes.size();
    }

    public Decimal getWasted()
    {
        return wasted;
    }

    public Counter<Party> getPartyElectedCounts()
    {
        return electedVotes.stream()
            .map(cv -> cv.getCandidate().getParty())
            .collect(Counter.collectCounts());
    }

    public Counter<Party> getPartyVoteCounts()
    {
        return partyVotes;
    }

    public Decimal getEffectiveVoteScore()
    {
        final var candidateSet = JImmutables.set(getElected());
        return effectiveBallots.getEffectiveVoteScore(candidateSet::contains);
    }

    // https://en.wikipedia.org/wiki/Gallagher_index
    public Decimal computeErrors()
    {
        final var partySeats = getPartyElectedCounts();
        final var totalSeats = partySeats.getTotal();
        final var totalVotes = partyVotes.getTotal();
        var sum = ZERO;
        for (Party party : getElection().getParties()) {
            final var seatPercentage = partySeats.get(party).dividedBy(totalSeats);
            final var votePercentage = partyVotes.get(party).dividedBy(totalVotes);
            final var diffSquared = votePercentage.minus(seatPercentage).squared();
            sum = sum.plus(diffSquared);
        }
        return sum.dividedBy(Decimal.TWO).root();
    }

    public Counter<Party> getPartyListSeats()
    {
        return electedVotes.stream()
            .filter(CandidateVotes::isList)
            .map(cv -> cv.getCandidate().getParty())
            .collect(Counter.collectCounts());
    }
}
