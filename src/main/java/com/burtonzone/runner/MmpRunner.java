package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.ResultsReport;
import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.DistrictMap;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import java.math.BigDecimal;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;

public class MmpRunner
    implements ElectionRunner
{
    private final Decimal MinPartyVoteRatio = new Decimal(new BigDecimal("0.05"));
    private final ElectionRunner districtRunner = PluralityRunner.singleVote();
    private final OpenListRunner partyRunner = new OpenListRunner(OpenListRunner.Config.builder()
                                                                      .seatAllocator(OpenListRunner.Config.PartySeatAllocator.Webster)
                                                                      .partyVoteMode(OpenListRunner.Config.PartyVoteMode.Voter)
                                                                      .listMode(OpenListRunner.Config.PartyListMode.Party)
                                                                      .quotasMode(OpenListRunner.Config.QuotasMode.None)
                                                                      .build());

    @Override
    public ElectionResult runElection(Election election)
    {
        return districtRunner.runElection(election);
    }

    @Override
    public Results runElections(Elections elections)
    {
        final var regionalElections = elections.splitRegions();
        RegionalResults combinedResults = null;
        for (Elections regionalElection : regionalElections) {
            final var regionalResults = runRegionalElections(regionalElection);
            if (combinedResults == null) {
                combinedResults = regionalResults;
            } else {
                combinedResults = combinedResults.add(regionalResults);
            }
        }
        assert combinedResults != null;
        return combinedResults.toElectionResults(elections.isParallel());
    }

    private RegionalResults runRegionalElections(Elections elections)
    {
        for (Election election : elections.getElections()) {
            if (election.getSeats() != 1) {
                throw new IllegalArgumentException("MMP requires single member districts");
            }
        }

        final var pluralityResults = districtRunner.runElections(elections);
        final var districts = elections.getElections();
        final var parties = districts.get(0).getParties();
        final var candidates = districts.stream()
            .flatMap(e -> e.getExpandedCandidateList().stream())
            .collect(listCollector());
        final var partyLists = Candidate.createPartyLists(parties, candidates);
        final var partyVotes = districts.transform(Election::getPartyVotes)
            .reduce(new Counter<Party>(), Counter::add);
        final var seats = computeSeatsWithPadding(districts.stream()
                                                      .mapToInt(Election::getSeats)
                                                      .sum());

        final var districtWinners = pluralityResults.getResults().stream()
            .flatMap(er -> er.getElected().stream())
            .collect(listCollector());

        final var ballots = districts.reduce(BallotBox.Empty, (sum, e) -> sum.add(e.getBallots()));
        final var filteredPartyVotes = applyPartyVoteFilters(partyVotes, districtWinners);
        final var partyElection = new Election("", parties, candidates, list(), partyLists, filteredPartyVotes, ballots, seats);
        final var partyResult = partyRunner.runMppPartyElection(partyElection, districtWinners);

        final var effectiveElection = new Election("", parties, candidates, list(), partyLists, partyVotes, ballots, seats);
        final var effectiveRoundResult = new ElectionResult.RoundResult(partyResult.getVotes(), partyResult.getElected());
        final var effectiveResults = new ElectionResult(effectiveElection,
                                                        list(effectiveRoundResult),
                                                        ballots,
                                                        partyVotes,
                                                        partyResult.getWasted());
        return new RegionalResults(elections.getElections(), pluralityResults.getResults(), list(effectiveResults));
    }

    private Counter<Party> applyPartyVoteFilters(Counter<Party> realResults,
                                                 JImmutableList<Candidate> districtWinners)
    {
        final var totalVotes = realResults.getTotal();
        final var minVotesToRemain = totalVotes.times(MinPartyVoteRatio).roundUp();
        final var electedParties = districtWinners.stream()
            .map(Candidate::getParty)
            .collect(setCollector());
        var filteredResults = new Counter<Party>();
        for (Counter.Entry<Party> entry : realResults) {
            final var party = entry.getKey();
            final var votes = entry.getCount();
            if (electedParties.contains(party) || votes.isGreaterOrEqualTo(minVotesToRemain)) {
                filteredResults = filteredResults.add(party, votes);
            }
        }
        return filteredResults;
    }

    @Override
    public int getSeatsForMap(DistrictMap districtMap)
    {
        return computeSeatsWithPadding(districtMap.getSeats());
    }

    private static int computeSeatsWithPadding(int seats)
    {
        return 1 + seats + seats / 2;
//        return seats;
    }

    @Value
    private static class RegionalResults
    {
        JImmutableList<Election> elections;
        JImmutableList<ElectionResult> pluralityResults;
        JImmutableList<ElectionResult> partyResults;

        private RegionalResults add(RegionalResults other)
        {
            return new RegionalResults(elections.insertAll(other.elections),
                                       pluralityResults.insertAll(other.pluralityResults),
                                       partyResults.insertAll(other.partyResults));
        }

        private Results toElectionResults(boolean parallel)
        {
            return new Results(new Elections(elections, parallel),
                               pluralityResults.insertAll(partyResults),
                               ResultsReport.of(partyResults));
        }
    }
}
