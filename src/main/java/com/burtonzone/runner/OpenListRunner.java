package com.burtonzone.runner;

import static com.burtonzone.common.Decimal.ZERO;
import static com.burtonzone.election.CandidateVotes.SelectionType.List;
import static com.burtonzone.election.CandidateVotes.SelectionType.Vote;
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
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;
import org.javimmutable.collections.JImmutableSetMap;
import org.javimmutable.collections.util.JImmutables;

public class OpenListRunner
    implements ElectionRunner
{
    private static final Comparator<CandidateVotes> CandidatesByPartyThenVotes =
        Comparator.<CandidateVotes, Party>comparing(cv -> cv.getCandidate().getParty(), Party.NameComparator)
            .thenComparing(Comparator.comparing(CandidateVotes::getVotes).reversed());

    private final Config config;

    public OpenListRunner(Config config)
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

    public ElectionResult runMppPartyElection(Election election,
                                              JImmutableList<Candidate> districtElected)
    {
        var worksheet = new Worksheet(election);
        worksheet.assignPreElectedCandidates(districtElected);
        worksheet.allocateSeatsToParties();
        worksheet.assignSeatsToCandidatesFromPartyLists();
        return worksheet.getElectionResult();
    }

    private class Worksheet
    {
        private final Election election;
        private final Counter<Candidate> candidateVotes;
        private final Counter<Party> candidatePartyVotes;
        private final JImmutableListMap<Party, Candidate> partyLists;
        private Counter<Party> partySeats;
        private JImmutableSetMap<Party, Candidate> elected;
        private JImmutableSetMap<Party, Candidate> voted;
        private JImmutableSetMap<Party, Candidate> selected;

        private Worksheet(Election election)
        {
            this.election = election;
            this.candidateVotes = election.getBallots().getCandidateFirstChoiceCounts();
            candidatePartyVotes = election.getBallots().getPartyFirstChoiceCounts();
            partyLists = selectPartyLists();
            partySeats = new Counter<>();
            elected = JImmutables.setMap();
            voted = JImmutables.setMap();
            selected = JImmutables.setMap();
        }

        private void allocateSeatsToParties()
        {
            var partySeats = new Counter<Party>();
            for (Party party : elected.keys()) {
                partySeats = partySeats.add(party, filledSeatsForParty(party));
            }
            if (config.seatAllocator == Config.PartySeatAllocator.Hare) {
                partySeats = computePartySeatsUsingHareQuotas(partySeats);
            } else {
                partySeats = computePartySeatsUsingFormula(partySeats);
            }
            assert partySeats.getTotal().toInt() == election.getSeats();
            this.partySeats = partySeats;
        }

        private Counter<Party> computePartySeatsUsingFormula(Counter<Party> partySeats)
        {
            final var totalSeats = new Decimal(election.getSeats());
            while (partySeats.getTotal().isLessThan(totalSeats)) {
                final var topParty = findPartyWithHighestAdjustedVotes(partySeats);
                partySeats = partySeats.inc(topParty);
            }
            return partySeats;
        }

        private Counter<Party> computePartySeatsUsingHareQuotas(Counter<Party> partySeats)
        {
            if (!partySeats.isEmpty()) {
                throw new IllegalArgumentException("Hare quotas cannot be used with pre-assigned seats.");
            }
            var partyVotes = selectPartyVotesForSeatAllocation();
            final var partyQuota = Election.computeQuota(partyVotes.getTotal(), election.getSeats());
            final var totalSeats = new Decimal(election.getSeats());
            for (var e : partyVotes) {
                var seats = e.getCount().div(partyQuota);
                if (seats.isGreaterThan(ZERO)) {
                    partySeats = partySeats.set(e.getKey(), seats);
                    partyVotes = partyVotes.subtract(e.getKey(), seats.times(partyQuota));
                }
            }
            var remaining = totalSeats.minus(partySeats.getTotal()).toInt();
            while (remaining > 0) {
                final var topParty = findPartyWithHighestVotes(partyVotes);
                partySeats = partySeats.inc(topParty);
                partyVotes = partyVotes.set(topParty, ZERO);
                remaining -= 1;
            }
            return partySeats;
        }

        private void assignPreElectedCandidates(JImmutableList<Candidate> preElected)
        {
            for (Candidate candidate : preElected) {
                addVoted(candidate);
            }
        }

        private void assignSeatsToCandidatesWithElectionQuota()
        {
            for (var entry : candidateVotes) {
                if (entry.getCount().isGreaterOrEqualTo(election.getQuota())) {
                    addVoted(entry.getKey());
                }
            }
        }

        private void assignSeatsToCandidatesWithPartyQuota()
        {
            final var sortedCandidates = candidateVotes.stream()
                .map(e -> new CandidateVotes(e.getKey(), e.getCount(), List))
                .sorted(CandidatesByPartyThenVotes)
                .collect(listCollector());
            Party currentParty = null;
            Decimal quota = null;
            for (var entry : sortedCandidates) {
                final var candidate = entry.getCandidate();
                final var party = candidate.getParty();
                if (!party.equals(currentParty)) {
                    var votes = Decimal.max(candidatePartyVotes.get(party), election.getPartyVotes().get(party));
                    currentParty = party;
                    quota = Election.computeQuota(votes, election.getSeats());
                }
                if (filledSeatsForParty(party) < numberOfSeatsForParty(party)) {
                    final var votes = entry.getVotes();
                    if (votes.isGreaterOrEqualTo(quota)) {
                        addVoted(candidate);
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
                        addSelected(candidate);
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
            var votedCandidateVotes = voted.stream()
                .flatMap(e -> e.getValue().stream())
                .map(c -> new CandidateVotes(c, candidateVotes.get(c), Vote))
                .collect(listCollector());
            var selectedCandidateVotes = selected.stream()
                .flatMap(e -> e.getValue().stream())
                .map(c -> new CandidateVotes(c, candidateVotes.get(c), List))
                .collect(listCollector());
            var electedCandidateVotes = votedCandidateVotes.insertAll(selectedCandidateVotes);
            if (electedCandidateVotes.size() != election.getSeats()) {
                throw new IllegalStateException(String.format("elected/seat mismatch: seats=%d elected=%d",
                                                              electedCandidateVotes.size(), election.getSeats()));
            }
            final var electedCandidates = electedCandidateVotes.transform(CandidateVotes::getCandidate);
            final var electedParties = electedCandidates.transform(set(), Candidate::getParty);
            final var wasted = election.getBallots()
                .withoutFirstChoiceMatching(c -> electedParties.contains(c.getParty()))
                .getTotalCount();
            final var round = new ElectionResult.RoundResult(electedCandidateVotes, electedCandidates);
            final var effectiveBallots = election.getBallots().toFirstChoicePartyBallots();
            return new ElectionResult(election, list(round), effectiveBallots, selectPartyVotesForSeatAllocation(), wasted);
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
            final var partyVotes = selectPartyVotesForSeatAllocation();
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

        private Party findPartyWithHighestVotes(Counter<Party> partyVotes)
        {
            Party topParty = null;
            Decimal topVotes = ZERO;
            for (Counter.Entry<Party> entry : partyVotes) {
                final Party party = entry.getKey();
                final Decimal rawVotes = entry.getCount();
                if (rawVotes.isGreaterThan(topVotes)) {
                    topVotes = rawVotes;
                    topParty = party;
                }
            }
            return topParty;
        }

        private Decimal computeAdjustedVotes(Decimal votes,
                                             Decimal seats)
        {
            return switch (config.seatAllocator) {
                case Webster -> votes.dividedBy(Decimal.ONE.plus(seats.times(Decimal.TWO)));
                case DHondt -> votes.dividedBy(Decimal.ONE.plus(seats));
                default -> throw new IllegalArgumentException("no formula for " + config.seatAllocator);
            };
        }

        private Counter<Party> selectPartyVotesForSeatAllocation()
        {
            return switch (config.partyVoteMode) {
                case Candidate -> candidatePartyVotes;
                case Voter -> election.getPartyVotes();
            };
        }

        private void addVoted(Candidate candidate)
        {
            final var party = candidate.getParty();
            if (!elected.getSet(party).contains(candidate)) {
                elected = elected.insert(party, candidate);
                voted = voted.insert(party, candidate);
            }
        }

        private void addSelected(Candidate candidate)
        {
            final var party = candidate.getParty();
            if (!elected.getSet(party).contains(candidate)) {
                elected = elected.insert(party, candidate);
                selected = selected.insert(party, candidate);
            }
        }
    }

    @Value
    @Builder
    public static class Config
    {
        public enum PartyVoteMode
        {
            /**
             * Computes party votes based on number of votes for candidates of each party.
             */
            Candidate,
            /**
             * Uses party votes from voter preferences.
             */
            Voter
        }

        @Builder.Default
        PartyVoteMode partyVoteMode = PartyVoteMode.Candidate;

        public enum PartyListMode
        {
            /**
             * Uses candidate votes to sort candidates into party lists.  Assigns order based solely on
             * voter preferences vs party preference.
             */
            Votes,
            /**
             * Uses party assigned party lists.  Assigns order based solely on party preference.
             */
            Party
        }

        @Builder.Default
        PartyListMode listMode = PartyListMode.Party;

        public enum PartySeatAllocator
        {
            // https://en.wikipedia.org/wiki/Webster/Sainte-Lagu%C3%AB_method
            Webster,
            DHondt,
            Hare
        }

        @Builder.Default
        PartySeatAllocator seatAllocator = PartySeatAllocator.Webster;

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
