package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.grid.GridElectionFactory;
import com.burtonzone.runner.basic_stv.BasicStvRunner;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class App
{
    private static final BigDecimal HUNDRED = new BigDecimal(100).setScale(8, RoundingMode.HALF_UP);

    public static void main(String[] args)
    {
        final var rand = new Rand();
//        final ElectionFactory factory = new LinearElectionFactory(rand);
        final ElectionFactory factory = new GridElectionFactory(rand, 5);
//        final var runner = OpenListFormulaRunner.dhondt();
        final var runner = new BasicStvRunner();
//        final var runner = new OpenListRunner();
//        final var runner = new SingleVoteRunner();
//        final var runner = new BlockPluralityRunner();
        for (int test = 1; test <= 23; ++test) {
            final var db = JImmutables.<DistrictSpec>listBuilder();
            // number and size of districts taken from fairvote.org plan for US house elections
            addDistricts(db, 44, 5);
            addDistricts(db, 9, 4);
            addDistricts(db, 54, 3);
            addDistricts(db, 5, 2);
            addDistricts(db, 7, 1);
            // double size districts
//            addDistricts(db, 44, 8);
//            addDistricts(db, 9, 6);
//            addDistricts(db, 54, 5);
//            addDistricts(db, 5, 3);
//            addDistricts(db, 7, 2);

            final JImmutableList<Election> elections =
                db.build()
                    .stream().parallel()
                    .map(spec -> spec.create(factory))
                    .collect(listCollector());

            final var results = elections
                .stream().parallel()
                .map(runner::runElection)
                .collect(listCollector());
            System.out.printf("%3s  %s%n", "", ResultsReport.printHeader1(factory.allParties()));
            System.out.printf("%3s  %s%n", "#", ResultsReport.printHeader2(factory.allParties()));
            for (ElectionResult result : results) {
                System.out.printf("%3d %s%n", test, ResultsReport.of(result).getRow());
            }
            System.out.printf("%3s %s%n", "TOT", ResultsReport.of(results).getRow());
            System.out.println();
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
