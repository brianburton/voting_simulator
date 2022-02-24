package com.burtonzone;

import com.burtonzone.election.ElectionResult;
import com.typesafe.config.ConfigFactory;

public class App
{
    public enum OutputMode
    {
        AllDistricts,
        TotalsOnly
    }

    public static void main(String[] args)
    {
        final var config = ConfigFactory.load();
        final var showDistrictResults = config.getEnum(OutputMode.class, "outputMode") == OutputMode.AllDistricts;
        final var scenario = Scenario.fromConfig(config);
        final var factory = scenario.getFactory();
        final var parties = scenario.getSettings().getParties();
        final var runner = scenario.getRunner();
        final var districts = scenario.getDistricts();

        for (int test = 1; test <= 10; ++test) {
            final var results = districts.parallelCreate(factory).run(runner);
            if (test == 1) {
                for (String row : results.getReport().getPartyDistanceGrid()) {
                    System.out.println(row);
                }
                System.out.println();
            }

            System.out.printf("%2s %s%n", "", results.getReport().printHeader1(parties));
            System.out.printf("%2s %s%n", "#", results.getReport().printHeader2(parties));
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

            for (String line : results.getReport().getCoalitionGrid(45)) {
                System.out.println("     " + line);
            }
            System.out.println();
        }
    }
}
