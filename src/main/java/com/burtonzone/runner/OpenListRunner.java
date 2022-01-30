package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.Ballot;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import java.util.Comparator;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

/**
 * Fully open list PR.
 */
public class OpenListRunner
    implements ElectionRunner
{
    @Override
    public ElectionResult runElection(Election election)
    {
        var worksheet = new Worksheet(election);
        for (Counter.Entry<Party> e : worksheet.partyVotes) {
            if (worksheet.isComplete()) {
                break;
            }
            var party = e.getKey();
            var votes = e.getCount();
            var numberToAdd = votes.div(election.getQuota());
            worksheet.electPartyCandidates(party, numberToAdd.toInt());
        }
        for (JImmutableMap.Entry<Party, Decimal> e : worksheet.getRemainders()) {
            if (worksheet.isComplete()) {
                break;
            }
            worksheet.electPartyCandidates(e.getKey(), 1);
        }
        return worksheet.getElectionResult();
    }

    private static class Worksheet
    {
        private final Election election;
        private final Counter<Party> partyVotes;
        private JImmutableList<CandidateVotes> unelectedCandidates;
        private JImmutableList<CandidateVotes> electedCandidates;

        private Worksheet(Election election)
        {
            var partyVotes = new Counter<Party>();
            var candidateVotes = new Counter<Candidate>();
            for (Counter.Entry<Ballot> e : election.getBallots().ballots()) {
                final var candidate = e.getKey().getFirstChoice();
                final var party = candidate.getParty();
                final var count = e.getCount();
                partyVotes = partyVotes.add(party, count);
                candidateVotes = candidateVotes.add(candidate, count);
            }
            this.election = election;
            this.partyVotes = partyVotes;
            unelectedCandidates = candidateVotes
                .getSortedList(election.getTieBreaker())
                .transform(CandidateVotes::new);
            electedCandidates = JImmutables.list();
        }

        private boolean isIncomplete()
        {
            return electedCandidates.size() < election.getSeats();
        }

        private boolean isComplete()
        {
            return !isIncomplete();
        }

        private void electPartyCandidates(Party party,
                                          int numberToAdd)
        {
            var i = 0;
            while (isIncomplete() && numberToAdd > 0 && i < unelectedCandidates.size()) {
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

        private JImmutableList<JImmutableMap.Entry<Party, Decimal>> getRemainders()
        {
            var reverseOrder = Comparator.<Decimal>naturalOrder().reversed();
            var remainders = JImmutables.<Decimal, Party>sortedListMap(reverseOrder);
            for (Counter.Entry<Party> e : partyVotes) {
                var remainder = e.getCount().mod(election.getQuota());
                remainders = remainders.insert(remainder, e.getKey());
            }
            return remainders.stream()
                .flatMap(e -> e.getValue().stream().map(p -> entry(p, e.getKey())))
                .collect(listCollector());
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
            return new ElectionResult(election, list(round));
        }
    }
}
