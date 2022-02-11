package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import java.util.function.BiFunction;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class OpenListFormulaRunner
    implements ElectionRunner
{
    public static final BiFunction<Decimal, Decimal, Decimal> DHondtFormula = (votes, seats) -> votes.dividedBy(Decimal.ONE.plus(seats));
    // https://en.wikipedia.org/wiki/Webster/Sainte-Lagu%C3%AB_method
    public static final BiFunction<Decimal, Decimal, Decimal> websterFormula = (votes, seats) -> votes.dividedBy(Decimal.ONE.plus(seats.times(Decimal.TWO)));

    private final BiFunction<Decimal, Decimal, Decimal> formula;

    public OpenListFormulaRunner(BiFunction<Decimal, Decimal, Decimal> formula)
    {
        this.formula = formula;
    }

    @Override
    public ElectionResult runElection(Election election)
    {
        var worksheet = new Worksheet(election);
        worksheet.countVotes();
        worksheet.assignSeats();
        return worksheet.getElectionResult();
    }

    private class Worksheet
    {
        private final Election election;
        private final Decimal seats;
        private final Counter<Party> partyVotes;
        private Counter<Party> partySeats;
        private JImmutableList<CandidateVotes> unelectedCandidates;
        private JImmutableList<CandidateVotes> electedCandidates;

        private Worksheet(Election election)
        {
            var partyVotes = new Counter<Party>();
            var candidateVotes = new Counter<Candidate>()
                .addZeros(election.getCandidates());
            for (var e : election.getBallots().getFirstChoiceCounts()) {
                final var candidate = e.getKey();
                final var party = candidate.getParty();
                final var count = e.getCount();
                partyVotes = partyVotes.add(party, count);
                candidateVotes = candidateVotes.add(candidate, count);
            }
            this.election = election;
            this.seats = new Decimal(election.getSeats());
            this.partyVotes = partyVotes;
            partySeats = new Counter<Party>();
            unelectedCandidates = candidateVotes
                .getSortedList(election.getTieBreaker())
                .transform(CandidateVotes::new);
            electedCandidates = JImmutables.list();
        }

        private void countVotes()
        {
            while (isIncomplete()) {
                var topParty = topParty();
                partySeats = partySeats.add(topParty, 1);
            }
        }

        private void assignSeats()
        {
            for (Counter.Entry<Party> entry : partySeats) {
                var party = entry.getKey();
                var count = entry.getCount().toInt();
                electPartyCandidates(party, count);
            }
        }

        private boolean isIncomplete()
        {
            return partySeats.getTotal().isLessThan(seats);
        }

        private Party topParty()
        {
            Party topParty = null;
            Decimal topVotes = Decimal.ZERO;
            for (Counter.Entry<Party> entry : partyVotes) {
                final Party party = entry.getKey();
                final Decimal rawVotes = entry.getCount();
                var adjustedVotes = formula.apply(rawVotes, partySeats.get(party));
                if (adjustedVotes.isGreaterThan(topVotes)) {
                    topVotes = adjustedVotes;
                    topParty = party;
                }
            }
            return topParty;
        }

        private void electPartyCandidates(Party party,
                                          int numberToAdd)
        {
            var i = 0;
            while (numberToAdd > 0 && i < unelectedCandidates.size()) {
                var cv = unelectedCandidates.get(i);
                if (cv.getCandidate().getParty().equals(party)) {
                    electedCandidates = electedCandidates.insertLast(cv);
                    unelectedCandidates = unelectedCandidates.delete(i);
                    numberToAdd -= 1;
                } else {
                    i += 1;
                }
            }
        }

        private Decimal getUnused()
        {
            var electedParties = electedCandidates
                .stream()
                .map(cv -> cv.getCandidate().getParty())
                .collect(setCollector());
            return unelectedCandidates
                .reject(cv -> electedParties.contains(cv.getCandidate().getParty()))
                .reduce(Decimal.ZERO, (s, cv) -> s.plus(cv.getVotes()));
        }

        private ElectionResult getElectionResult()
        {
            final var elected = electedCandidates.transform(CandidateVotes::getCandidate);
            final var exhausted = getUnused();
            final var round = new ElectionResult.RoundResult(electedCandidates, elected, exhausted);
            final var effectiveBallots = election.getBallots().toFirstChoicePartyBallots();
            return new ElectionResult(election, list(round), effectiveBallots);
        }
    }
}
