package com.burtonzone;

import com.burtonzone.common.Rand;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.ElectionSettings;
import com.burtonzone.election.IssueSpaces;
import com.burtonzone.election.PositionalElectionFactory;

public class App
{
    public static void main(String[] args)
    {
        final var showDistrictResults = false;
        final var rand = new Rand();
        final var issueSpace = IssueSpaces.grid(rand);
        final var factory = new PositionalElectionFactory(rand, issueSpace);
        final int numParties = 3;
        final var parties = factory.createParties(numParties);
        final var electionSettings =
            ElectionSettings.builder()
                .parties(parties)
                .maxCandidateChoices(Integer.MAX_VALUE)
                .maxPartyChoices(Math.max(1, parties.size() / 2))
//                .voteType(ElectionSettings.VoteType.Candidate)
//                .voteType(ElectionSettings.VoteType.Party)
                .voteType(ElectionSettings.VoteType.Mixed)
                .build();

//        ElectionRunner runner = Runners.hare();
//        ElectionRunner runner = Runners.dhondt();
//        ElectionRunner runner = Runners.webster();
//        runner = Runners.hybrid(runner);
//        ElectionRunner runner = Runners.basicStv();
        ElectionRunner runner = Runners.singleVote();
//        ElectionRunner runner = Runners.blockVote();

        final var districts =
//            DistrictMaps.congressFairVote(electionSettings);
            DistrictMaps.marylandDelegatesMax7(electionSettings);

        for (int test = 1; test <= 10; ++test) {
            final var results = districts.parallelCreate(factory).run(runner);
            if (test == 1) {
                for (String row : results.getReport().getPartyDistanceGrid()) {
                    System.out.println(row);
                }
                System.out.println();
            }

            System.out.printf("%2s %s%n", "", ResultsReport.printHeader1(parties));
            System.out.printf("%2s %s%n", "#", ResultsReport.printHeader2(parties));
            if (showDistrictResults) {
                for (ElectionResult result : results.getResults()) {
                    final ResultsReport districtReport = ResultsReport.of(result);
                    System.out.printf("%2d %s%n", test, districtReport.getRow1());
                    System.out.printf("%2s %s%n", "", districtReport.getRow2());
                }
            }
            System.out.printf("%2s %s%n",
                              showDistrictResults ? "TL" : "" + test,
                              results.getReport().getRow1());
            System.out.printf("%2s %s%n", "", results.getReport().getRow2());
            if (showDistrictResults) {
                System.out.println();
            }

            for (String line : results.getReport().getCoalitionGrid(40)) {
                System.out.println("     " + line);
            }
            System.out.println();
        }
    }
}
