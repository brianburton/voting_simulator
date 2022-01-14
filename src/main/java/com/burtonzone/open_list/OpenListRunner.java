package com.burtonzone.open_list;

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
        var partyVotes = new Counter<Party>();
        var candidateVotes = new Counter<Candidate>();
        for (Counter.Entry<Ballot> e : election.getBallots().ballots()) {
            var candidate = e.getKey().getFirstChoice();
            var count = e.getCount();
            partyVotes = partyVotes.add(candidate.getParty(), count);
            candidateVotes = candidateVotes.add(candidate, count);
        }
        var sortedCandidates = candidateVotes.getSortedList(election.getTieBreaker());
        var totalVotes = election.getTotalVotes();
        var elected = JImmutables.<CandidateVotes>list();
        for (Counter.Entry<Party> e : partyVotes) {
            var party = e.getKey();
            var votes = e.getCount();
            var remaining = votes.div(election.getQuota()).toInt();
            var i = 0;
            while (remaining > 0 && i < sortedCandidates.size()) {
                var c = sortedCandidates.get(i);
                if (c.getKey().getParty().equals(party)) {
                    elected = elected.insertLast(new CandidateVotes(c));
                    sortedCandidates = sortedCandidates.delete(i);
                    remaining -= 1;
                } else {
                    i += 1;
                }
            }
        }
        if (elected.size() < election.getSeats()) {
            var reverseOrder = Comparator.<Decimal>naturalOrder().reversed();
            var remainders = JImmutables.<Decimal, Candidate>sortedListMap(reverseOrder);
            for (Counter.Entry<Candidate> e : sortedCandidates) {
                var remainder = e.getCount().mod(election.getQuota());
                remainders = remainders.insert(remainder, e.getKey());
            }
            for (JImmutableMap.Entry<Decimal, JImmutableList<Candidate>> re : remainders) {
                for (Candidate candidate : re.getValue()) {
                    if (election.getSeats() > elected.size()) {
                        elected = elected.insertLast(new CandidateVotes(candidate, re.getKey()));
                    }
                }
            }
        }
        final var electedCandidates = elected.transform(CandidateVotes::getCandidate);
        return new ElectionResult(election, list(new ElectionResult.RoundResult(elected, electedCandidates, Decimal.ZERO)));
    }
}
