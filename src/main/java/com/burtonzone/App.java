package com.burtonzone;

import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionSettings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;

public class App
{
    public enum OutputMode
    {
        Districts,
        Totals
    }

    public static void main(String[] args)
    {
        final var config = loadConfigs(args);
        final var scenario = Scenario.fromConfig(config);
        final var factory = scenario.getFactory();
        final var parties = scenario.getSettings().getParties();
        final var runner = scenario.getRunner();
        final var districts = scenario.getDistricts();
        final var parallelExecution = config.getBoolean("parallelExecution");
        final var showDistrictResults = config.getEnum(OutputMode.class, "outputMode") == OutputMode.Districts;
        final var numberOfRounds = showDistrictResults ? 1 : config.getInt("numberOfRounds");

        System.out.printf("Voting System        : %s%n", scenario.getVotingSystem());
        System.out.printf("Ranking Method       : %s%n", scenario.getSettings().getVoteType());
        System.out.printf("District Map         : %s%n", scenario.getDistrictMap());
        System.out.printf("Total Seats          : %s%n", scenario.getSeats());
        System.out.printf("Max Party Choices    : %d%n", scenario.getSettings().getMaxPartyChoices());
        System.out.printf("Max Candidate Choices: %s%n", scenario.getSettings().getMaxCandidateChoices());
        System.out.printf("Mixed Party Vote %%   : %s%n", scenario.getSettings().getVoteType() == ElectionSettings.VoteType.Mixed
                                                          ? String.valueOf(scenario.getSettings().getMixedPartyVotePercentage()) : "n/a");
        System.out.println();

        for (String row : ResultsReport.getPartyDistanceGrid(parties)) {
            System.out.println(row);
        }
        System.out.println();

        for (int roundNumber = 1; roundNumber <= numberOfRounds; ++roundNumber) {
            final var results = runner.runElections(districts.create(factory, parallelExecution));
            System.out.printf("%2s %s%n", "", results.getReport().printHeader1(parties));
            System.out.printf("%2s %s%n", "#", results.getReport().printHeader2(parties));
            if (showDistrictResults) {
                for (ElectionResult result : results.getResults()) {
                    final ResultsReport districtReport = ResultsReport.of(result);
                    System.out.printf("%2d %s%n", roundNumber, districtReport.getRow1());
                    System.out.printf("%2s %s%n", "", districtReport.getRow2());
                }
            }
            System.out.printf("%2s %s%n",
                              showDistrictResults ? "TL" : "" + roundNumber,
                              results.getReport().getRow1());
            System.out.printf("%2s %s%n", "", results.getReport().getRow2());
            if (showDistrictResults) {
                System.out.println();
            }

            for (String line : results.getReport().getCoalitionGrid(45)) {
                System.out.println("     " + line);
            }
            System.out.println();
        }
    }

    private static Config loadConfigs(String[] argv)
    {
        var root = new File(".");
        var configDir = new File("configs");
        if (configDir.isDirectory()) {
            root = configDir;
        }
        var config = ConfigFactory.load();
        for (String arg : argv) {
            var file = new File(root, arg);
            if (file.isFile() && file.getName().endsWith(".conf")) {
                var fileConfig = ConfigFactory.parseFile(file);
                config = fileConfig.withFallback(config);
            }
        }
        config = ConfigFactory
            .parseProperties(System.getProperties())
            .withFallback(config);
        return config;
    }
}
