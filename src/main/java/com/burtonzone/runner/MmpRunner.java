package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.ResultsReport;
import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.CandidateVotes;
import com.burtonzone.election.DistrictMap;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import java.math.BigDecimal;
import java.util.function.IntUnaryOperator;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;

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
    private final IntUnaryOperator seatsCalculator;

    public MmpRunner(IntUnaryOperator seatsCalculator)
    {
        this.seatsCalculator = seatsCalculator;
    }

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
        final int districtSeats = districts.stream()
            .mapToInt(Election::getSeats)
            .sum();
        final var seats = seatsCalculator.applyAsInt(districtSeats);

        final var districtWinners = pluralityResults.getResults().stream()
            .flatMap(er -> er.getElectedVotes().stream())
            .map(CandidateVotes::getCandidate)
            .collect(listCollector());

        final var filteredParties = computePartiesToFilter(partyVotes, districtWinners);
        final var ballots = districts
            .reduce(BallotBox.Empty, (sum, e) -> sum.add(e.getBallots()))
            .toSingleChoiceBallots();
        final var partyBallots = ballots
            .withoutBallotsMatching(b -> b.partyIn(filteredParties));
        final var partyElection = new Election("", parties, candidates, list(), partyLists, partyBallots, seats);
        final var partyResult = partyRunner.runMppPartyElection(partyElection, districtWinners);

        int votedCount = CandidateVotes.countType(partyResult.getElectedVotes(), CandidateVotes.SelectionType.Vote);
        int listCount = CandidateVotes.countType(partyResult.getElectedVotes(), CandidateVotes.SelectionType.List);
        if (votedCount != districtWinners.size()) {
            throw new IllegalArgumentException("voted count mismatch");
        }
        if (seats != votedCount + listCount) {
            throw new IllegalArgumentException("count mismatch");
        }
        if (listCount != seats - districtSeats) {
            throw new IllegalArgumentException("list count mismatch");
        }
        final var winningCandidates = set(districtWinners);
        final var winningParties = partyResult.getElectedVotes()
            .transform(set(), cv -> cv.getCandidate().getParty());
        final var effectiveElection = new Election("", parties, candidates, list(), partyLists, ballots, seats);
        final var wasted = ballots.countWastedUsingCandidateOrParty(partyResult.getElectedVotes());
        final var effectiveVoteScore = OpenListRunner.computeEffectiveVoteScore(ballots, winningCandidates, winningParties);
        final var effectiveResults = new ElectionResult(effectiveElection,
                                                        ballots,
                                                        partyVotes,
                                                        wasted,
                                                        effectiveVoteScore,
                                                        partyResult.getElectedVotes());
        if (CandidateVotes.countType(effectiveResults.getElectedVotes(), CandidateVotes.SelectionType.Vote) != votedCount) {
            throw new IllegalArgumentException("voted count mismatch");
        }
        if (CandidateVotes.countType(effectiveResults.getElectedVotes(), CandidateVotes.SelectionType.List) != listCount) {
            throw new IllegalArgumentException("count mismatch");
        }
        return new RegionalResults(elections.getElections(), pluralityResults.getResults(), list(effectiveResults));
    }

    private JImmutableSet<Party> computePartiesToFilter(Counter<Party> realVotes,
                                                        JImmutableList<Candidate> districtWinners)
    {
        final var totalVotes = realVotes.getTotal();
        final var minVotesToRemain = totalVotes.times(MinPartyVoteRatio).roundUp();
        final var electedParties = districtWinners.stream()
            .map(Candidate::getParty)
            .collect(setCollector());
        JImmutableSet<Party> filteredResults = set();
        for (Counter.Entry<Party> entry : realVotes) {
            final var party = entry.getKey();
            final var votes = entry.getCount();
            final boolean shouldRetain = electedParties.contains(party) || votes.isGreaterOrEqualTo(minVotesToRemain);
            if (!shouldRetain) {
                filteredResults = filteredResults.insert(party);
            }
        }
        return filteredResults;
    }

    @Override
    public int getSeatsForMap(DistrictMap districtMap)
    {
        return districtMap.getRegionSeats().stream()
            .mapToInt(entry -> entry.getCount().toInt())
            .map(seatsCalculator)
            .sum();
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
