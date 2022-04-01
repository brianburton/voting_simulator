package com.burtonzone.runner;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.ResultsReport;
import com.burtonzone.common.Counter;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.Candidate;
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
        final var parties = elections.getElections().get(0).getParties();
        final var candidates = elections.getElections().stream()
            .flatMap(e -> e.getCandidates().stream())
            .collect(listCollector());
        final var partyLists = Candidate.createPartyLists(parties, candidates);
        final var partyVotes = elections.getElections().stream()
            .map(e -> e.getPartyVotes())
            .collect(listCollector())
            .reduce(new Counter<Party>(), Counter::add);
        final var seats = elections.getElections().stream()
            .mapToInt(Election::getSeats)
            .sum();
        var districtResults = districtRunner.runElections(elections);
        var ballots = elections.getElections().reduce(BallotBox.builder().build(), (sum, e) -> sum.add(e.getBallots()));
        var partyElection = new Election(parties, candidates, partyLists, partyVotes, ballots, seats);
        var districtWinners = districtResults.getResults().stream()
            .flatMap(er -> er.getElected().stream())
            .collect(listCollector());
        var partyResult = partyRunner.runMppPartyElection(partyElection, districtWinners);
        return new Results(elections, list(partyResult), ResultsReport.of(partyResult));
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
