package com.burtonzone.runner;

import static com.burtonzone.common.Decimal.ZERO;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import java.util.Comparator;
import lombok.Builder;
import lombok.Value;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.JImmutableSetMap;
import org.javimmutable.collections.util.JImmutables;

public class OpenListFormulaRunner
    implements ElectionRunner
{
    private static final Comparator<CandidateVotes> CandidatesByPartyThenVotes =
        Comparator.<CandidateVotes, Party>comparing(cv -> cv.getCandidate().getParty(), Party.NameComparator)
            .thenComparing(Comparator.comparing(CandidateVotes::getVotes).reversed());

    private final Config config;

    public OpenListFormulaRunner(Config config)
    {
        this.config = config;
    }

    @Override
    public ElectionResult runElection(Election election)
    {
        var worksheet = new Worksheet(election);
        if (config.getQuotasMode().isTotalQuotaEnabled()) {
            worksheet.assignSeatsToCandidatesWithElectionQuota();
        }
        worksheet.allocateSeatsToParties();
        if (config.getQuotasMode().isPartyQuotaEnabled()) {
            worksheet.assignSeatsToCandidatesWithPartyQuota();
        }
        worksheet.assignSeatsToCandidatesFromPartyLists();
        return worksheet.getElectionResult();
    }

    private class Worksheet
    {
        private final Election election;
        private final Counter<Party> partyVotes;
        private final Counter<Candidate> candidateVotes;
        private final JImmutableListMap<Party, Candidate> partyLists;
        private Counter<Party> partySeats;
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
            this.partyVotes = selectPartyVotes(election, partyVotes);
            this.candidateVotes = candidateVotes;
            partyLists = selectPartyLists();
            partySeats = new Counter<>();
            elected = JImmutables.setMap();
        }

        private Counter<Party> selectPartyVotes(Election election,
                                                Counter<Party> candidatePartyCounts)
        {
            return switch (config.partyVoteMode) {
                case Candidate -> candidatePartyCounts;
                case Voter -> election.getPartyVotes();
            };
        }

        private void allocateSeatsToParties()
        {
            var partySeats = new Counter<Party>();
            for (Party party : elected.keys()) {
                partySeats = partySeats.add(party, filledSeatsForParty(party));
            }
            final var totalSeats = new Decimal(election.getSeats());
            while (partySeats.getTotal().isLessThan(totalSeats)) {
                final var topParty = findPartyWithHighestAdjustedVotes(partySeats);
                partySeats = partySeats.inc(topParty);
            }
            assert partySeats.getTotal().toInt() == election.getSeats();
            this.partySeats = partySeats;
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
            final var sortedCandidates = candidateVotes.stream()
                .map(e -> new CandidateVotes(e.getKey(), e.getCount()))
                .sorted(CandidatesByPartyThenVotes)
                .collect(listCollector());
            Party currentParty = null;
            Decimal quota = null;
            for (var entry : sortedCandidates) {
                final var candidate = entry.getCandidate();
                final var party = candidate.getParty();
                if (!party.equals(currentParty)) {
                    currentParty = party;
                    quota = Election.computeQuota(partyVotes.get(party), election.getSeats());
                }
                if (filledSeatsForParty(party) < numberOfSeatsForParty(party)) {
                    final var votes = entry.getVotes();
                    if (votes.isGreaterOrEqualTo(quota)) {
                        elected = elected.insert(party, candidate);
                    }
                }
            }
        }

        private void assignSeatsToCandidatesFromPartyLists()
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
            Decimal topVotes = ZERO;
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
        public enum PartyVoteMode
        {
            Candidate,
            Voter
        }

        @Builder.Default
        PartyVoteMode partyVoteMode = PartyVoteMode.Candidate;

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
