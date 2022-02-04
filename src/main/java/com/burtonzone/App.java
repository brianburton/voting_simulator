package com.burtonzone;

import static com.burtonzone.Runners.basicStv;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.grid.GridElectionFactory;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class App
{
    public static void main(String[] args)
    {
        final var showDistrictResults = false;
        final var rand = new Rand(1);
        final ElectionFactory factory = new GridElectionFactory(rand, 5);
//        ElectionRunner runner = dhondt();
//        ElectionRunner runner = hare();
//        ElectionRunner runner = webster();
        ElectionRunner runner = basicStv();
//        ElectionRunner runner = singleVote();
//        ElectionRunner runner = blockPlurality();
//        runner = hybrid(runner);

        for (int test = 1; test <= 23; ++test) {
            final var db = JImmutables.<DistrictSpec>listBuilder();
            // all districts single seat to simulate current system
//            addDistricts(db, 435, 1);

            // number and size of districts taken from fairvote.org plan for US house elections
            addDistricts(db, 44, 5);
            addDistricts(db, 9, 4);
            addDistricts(db, 54, 3);
            addDistricts(db, 5, 2);
            addDistricts(db, 7, 1);

            // larger districts
//            addDistricts(db, 44, 9);
//            addDistricts(db, 9, 7);
//            addDistricts(db, 54, 5);
//            addDistricts(db, 5, 3);
//            addDistricts(db, 7, 2);

            final JImmutableList<Election> elections =
                db.build()
                    .stream() //.parallel()
                    .map(spec -> spec.create(factory))
                    .collect(listCollector());

            final var results = elections
                .stream().parallel()
                .map(runner::runElection)
                .collect(listCollector());
            if (showDistrictResults || test == 1) {
                System.out.printf("%3s  %s%n", "", ResultsReport.printHeader1(factory.allParties()));
                System.out.printf("%3s  %s%n", "#", ResultsReport.printHeader2(factory.allParties()));
            }
            if (showDistrictResults) {
                for (ElectionResult result : results) {
                    System.out.printf("%3d %s%n", test, ResultsReport.of(result).getRow());
                }
            }
            System.out.printf("%3s %s%n",
                              showDistrictResults ? "TOT" : "" + test,
                              ResultsReport.of(results).getRow());
            if (showDistrictResults) {
                System.out.println();
            }
        }
    }

    private static void addDistricts(JImmutableList.Builder<DistrictSpec> db,
                                     int numberOfDistricts,
                                     int numberOfSeatsPerDistrict)
    {
        final var districtSpec = new DistrictSpec(numberOfSeatsPerDistrict);
        for (int i = 1; i <= numberOfDistricts; ++i) {
            db.add(districtSpec);
        }
    }

    @Value
    private static class DistrictSpec
    {
        int numberOfSeats;

        Election create(ElectionFactory factory)
        {
            return factory.createElection(numberOfSeats);
        }
    }
}
