package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.ElectionSettings;
import com.burtonzone.grid.GridElectionFactory;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class App
{
    public static void main(String[] args)
    {
        final var showDistrictResults = false;
        final var rand = new Rand();
        final var electionSettings =
            ElectionSettings.builder()
                .voteType(ElectionSettings.VoteType.Candidate)
                .build();

        final ElectionFactory factory = new GridElectionFactory(rand, 6);
//        ElectionRunner runner = Runners.hare();
//        ElectionRunner runner = Runners.dhondt();
        ElectionRunner runner = Runners.webster();
        runner = Runners.hybrid(runner);
//        ElectionRunner runner = Runners.basicStv();
//        ElectionRunner runner = Runners.singleVote();


//        ElectionRunner runner = Runners.blockPlurality();

        for (int test = 1; test <= 10; ++test) {
            final var db = JImmutables.<DistrictSpec>listBuilder();
            // all districts single seat to simulate current system
//            addDistricts(db, 435, 1);

            // number and size of districts taken from fairvote.org plan for US house elections
            addDistricts(db, 44, electionSettings.withNumberOfSeats(5));
            addDistricts(db, 9, electionSettings.withNumberOfSeats(4));
            addDistricts(db, 54, electionSettings.withNumberOfSeats(3));
            addDistricts(db, 5, electionSettings.withNumberOfSeats(2));
            addDistricts(db, 7, electionSettings.withNumberOfSeats(1));

            // larger districts
//            addDistricts(db, 44, 9);
//            addDistricts(db, 9, 7);
//            addDistricts(db, 54, 5);
//            addDistricts(db, 5, 3);
//            addDistricts(db, 7, 2);

            final JImmutableList<Election> elections =
                db.build()
                    .stream()//.parallel()
                    .map(spec -> spec.create(factory))
                    .collect(listCollector());

            final var results = elections
                .stream().parallel()
                .map(runner::runElection)
                .collect(listCollector());

            final ResultsReport resultsReport = ResultsReport.of(results);
            if (test == 1) {
                for (String row : resultsReport.getPartyDistanceGrid()) {
                    System.out.println(row);
                }
                System.out.println();
            }

            if (showDistrictResults || test == 1) {
                System.out.printf("%3s %s%n", "", ResultsReport.printHeader1(factory.allParties()));
                System.out.printf("%3s %s%n", "#", ResultsReport.printHeader2(factory.allParties()));
            }
            if (showDistrictResults) {
                for (ElectionResult result : results) {
                    System.out.printf("%3d %s%n", test, ResultsReport.of(result).getRow());
                }
            }
            System.out.printf("%3s %s%n",
                              showDistrictResults ? "TOT" : "" + test,
                              resultsReport.getRow());
            if (showDistrictResults) {
                System.out.println();
            }
        }
    }

    private static void addDistricts(JImmutableList.Builder<DistrictSpec> db,
                                     int numberOfDistricts,
                                     ElectionSettings settings)
    {
        final var districtSpec = new DistrictSpec(settings);
        for (int i = 1; i <= numberOfDistricts; ++i) {
            db.add(districtSpec);
        }
    }

    @Value
    private static class DistrictSpec
    {
        ElectionSettings settings;

        Election create(ElectionFactory factory)
        {
            return factory.createElection(settings);
        }
    }
}
