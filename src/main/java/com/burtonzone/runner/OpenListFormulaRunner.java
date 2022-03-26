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
import lombok.Builder;
import lombok.Value;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.JImmutableSetMap;
import org.javimmutable.collections.util.JImmutables;

public class OpenListFormulaRunner
    implements ElectionRunner
{
    private final Config config;

    public OpenListFormulaRunner(Config config)
    {
        this.config = config;
    }

    @Override
    public ElectionResult runElection(Election election)
    {
        var worksheet = new Worksheet(election);
        worksheet.computePartySeatsFromVotes();
        if (config.getQuotasMode().isTotalQuotaEnabled()) {
            worksheet.assignSeatsToCandidatesWithElectionQuota();
        }
        if (config.getQuotasMode().isPartyQuotaEnabled()) {
            worksheet.assignSeatsToCandidatesWithPartyQuota();
        }
        worksheet.assignSeatsFromPartyLists();
        return worksheet.getElectionResult();
    }

    private class Worksheet
    {
        private final Election election;
        private final Counter<Party> partyVotes;
        private final Counter<Candidate> candidateVotes;
        private final JImmutableListMap<Party, Candidate> partyLists;
        private final Counter<Party> partySeats;
        private JImmutableSetMap<Party, Candidate> elected;

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
            this.partyVotes = partyVotes;
            this.candidateVotes = candidateVotes;
            partyLists = selectPartyLists();
            partySeats = computePartySeatsFromVotes();
            elected = JImmutables.setMap();
        }

        private Counter<Party> computePartySeatsFromVotes()
        {
            var partySeats = new Counter<Party>();
            var remainingSeats = election.getSeats() - partySeats.getTotal().toInt();
            while (remainingSeats > 0) {
                final var topParty = findPartyWithHighestAdjustedVotes(partySeats);
                partySeats = partySeats.add(topParty, 1);
                remainingSeats -= 1;
            }
            assert partySeats.getTotal().toInt() == election.getSeats();
            return partySeats;
        }

        private void assignSeatsToCandidatesWithElectionQuota()
        {
            for (var entry : candidateVotes) {
                if (entry.getCount().isGreaterOrEqualTo(election.getQuota())) {
                    final var candidate = entry.getKey();
                    final var party = candidate.getParty();
                    elected = elected.insert(party, candidate);
                }
            }
        }

        private void assignSeatsToCandidatesWithPartyQuota()
        {
            var partyQuotas = new Counter<Party>();
            for (var entry : partyVotes) {
                final var party = entry.getKey();
                final var totalVotes = entry.getCount();
                final var numberOfSeats = numberOfSeatsForParty(party);
                final var quota = Election.computeQuota(totalVotes, numberOfSeats);
                partyQuotas = partyQuotas.set(party, quota);
            }
            for (var entry : candidateVotes.getSortedList()) {
                final var candidate = entry.getKey();
                final var party = candidate.getParty();
                if (filledSeatsForParty(party) < numberOfSeatsForParty(party)) {
                    final var votes = entry.getCount();
                    final var quota = partyQuotas.get(party);
                    if (votes.isGreaterOrEqualTo(quota)) {
                        elected = elected.insert(party, candidate);
                    }
                }
            }
        }

        private void assignSeatsFromPartyLists()
        {
            for (var entry : partySeats) {
                final var party = entry.getKey();
                final var numberOfSeats = entry.getCount().toInt();
                for (var candidate : partyLists.getList(party)) {
                    if (filledSeatsForParty(party) < numberOfSeats) {
                        elected = elected.insert(party, candidate);
                    }
                }
                assert filledSeatsForParty(party) == numberOfSeats;
            }
        }

        private int numberOfSeatsForParty(Party party)
        {
            return partySeats.get(party).toInt();
        }

        private int filledSeats()
        {
            return elected.stream()
                .mapToInt(e -> e.getValue().size())
                .sum();
        }

        private int filledSeatsForParty(Party party)
        {
            return elected.getSet(party).size();
        }

        private ElectionResult getElectionResult()
        {
            if (filledSeats() != election.getSeats()) {
                throw new IllegalStateException(String.format("total elected/seat mismatch: seats=%d elected=%d",
                                                              filledSeats(), election.getSeats()));
            }
            for (Party party : election.getParties()) {
                final var allocatedSeats = numberOfSeatsForParty(party);
                final var filledSeats = filledSeatsForParty(party);
                if (allocatedSeats != filledSeats) {
                    throw new IllegalStateException(String.format("elected/seat mismatch for party: seats=%d elected=%d party=%s",
                                                                  allocatedSeats, filledSeats, party));
                }
            }
            var electedCandidates = elected.stream()
                .flatMap(e -> e.getValue().stream())
                .map(c -> new CandidateVotes(c, candidateVotes.get(c)))
                .collect(listCollector());
            if (electedCandidates.size() != election.getSeats()) {
                throw new IllegalStateException(String.format("elected/seat mismatch: seats=%d elected=%d",
                                                              electedCandidates.size(), election.getSeats()));
            }
            return ElectionResult.ofPartyListResults(election, electedCandidates);
        }

        private JImmutableListMap<Party, Candidate> selectPartyLists()
        {
            return switch (config.listMode) {
                case Votes -> candidateVotes
                    .getSortedList(election.getTieBreaker())
                    .stream()
                    .map(e -> entry(e.getKey().getParty(), e.getKey()))
                    .collect(listMapCollector());
                case Party -> election.getPartyLists();
            };
        }

        private Party findPartyWithHighestAdjustedVotes(Counter<Party> partySeats)
        {
            Party topParty = null;
            Decimal topVotes = Decimal.ZERO;
            for (Counter.Entry<Party> entry : partyVotes) {
                final Party party = entry.getKey();
                final Decimal rawVotes = entry.getCount();
                var adjustedVotes = computeAdjustedVotes(rawVotes, partySeats.get(party));
                if (adjustedVotes.isGreaterThan(topVotes)) {
                    topVotes = adjustedVotes;
                    topParty = party;
                }
            }
            return topParty;
        }

        private Decimal computeAdjustedVotes(Decimal votes,
                                             Decimal seats)
        {
            return switch (config.formula) {
                case Webster -> votes.dividedBy(Decimal.ONE.plus(seats.times(Decimal.TWO)));
                case DHondt -> votes.dividedBy(Decimal.ONE.plus(seats));
            };
        }
    }

    @Value
    @Builder
    public static class Config
    {
        public enum PartyListMode
        {
            Votes,
            Party
        }

        @Builder.Default
        PartyListMode listMode = PartyListMode.Party;

        public enum PartySeatFormula
        {
            // https://en.wikipedia.org/wiki/Webster/Sainte-Lagu%C3%AB_method
            Webster,
            DHondt
        }

        @Builder.Default
        PartySeatFormula formula = PartySeatFormula.Webster;

        public enum QuotasMode
        {
            None,
            TotalOnly,
            PartyOnly,
            TotalAndParty;

            private boolean isTotalQuotaEnabled()
            {
                return this == TotalOnly || this == TotalAndParty;
            }

            private boolean isPartyQuotaEnabled()
            {
                return this == PartyOnly || this == TotalAndParty;
            }
        }

        @Builder.Default
        QuotasMode quotasMode = QuotasMode.None;
    }
}
