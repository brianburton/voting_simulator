package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.ResultsReport;
import com.burtonzone.common.Counter;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.DistrictMap;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.Party;
import org.javimmutable.collections.JImmutableList;

public class MmpRunner
    implements ElectionRunner
{
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
        for (Election election : elections.getElections()) {
            if (election.getSeats() != 1) {
                throw new IllegalArgumentException("MMP requires single member districts");
            }
        }
        final var districts = elections.getElections();
        final var parties = districts.get(0).getParties();
        final var candidates = districts.stream()
            .flatMap(e -> e.getExpandedCandidateList().stream())
            .collect(listCollector());
        final var partyLists = Candidate.createPartyLists(parties, candidates);
        final var partyVotes = districts.stream()
            .map(Election::getPartyVotes)
            .collect(listCollector())
            .reduce(new Counter<Party>(), Counter::add);
        final var seats = 2 * districts.stream()
            .mapToInt(Election::getSeats)
            .sum();
        final var districtResults = districtRunner.runElections(elections);
        final var ballots = districts.reduce(BallotBox.Empty, (sum, e) -> sum.add(e.getBallots()));
        final var partyElection = new Election(parties, candidates, list(), partyLists, partyVotes, ballots, seats);
        final var districtWinners = districtResults.getResults().stream()
            .flatMap(er -> er.getElected().stream())
            .collect(listCollector());
        final var partyResult = partyRunner.runMppPartyElection(partyElection, districtWinners);
        final var combinedResults = districtResults.getResults().insert(partyResult);
        return new Results(elections, combinedResults, ResultsReport.of(partyResult));
    }

    @Override
    public int getSeatsForMap(DistrictMap districtMap)
    {
        return 2 * districtMap.getSeats();
    }

    private JImmutableList<Candidate> computePreElected(Elections elections)
    {
        return runElections(elections)
            .getResults()
            .stream()
            .flatMap(r -> r.getElected().stream())
            .collect(listCollector());
    }
}
