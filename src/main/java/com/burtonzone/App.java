package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.Party;
import com.burtonzone.grid.GridElectionFactory;
import com.burtonzone.runner.OpenListRunner;
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
        final var rand = new Rand(1);
//        final ElectionFactory factory = new LinearElectionFactory(rand);
        final ElectionFactory factory = new GridElectionFactory(rand, 3);
//        final var runner = new BasicStvRunner();
        final var runner = new OpenListRunner();
//        final var runner = new SingleVoteRunner();
        System.out.printf("%-3s", "");
        System.out.printf(" %-3s %-3s %-3s %-3s %-3s   ",
                          "",
                          "",
                          "",
                          "",
                          "");
        for (Party party : factory.allParties()) {
            System.out.printf(" %s ", center(party.getName(), 19));
        }
        System.out.println();
        System.out.printf("%3s", "#");
        System.out.printf(" %3s %3s %3s %3s %3s   ",
                          "ec",
                          "sc",
                          "rc",
                          "rsc",
                          "rec");
        for (Party party : factory.allParties()) {
            System.out.printf(" %4s   %5s  %5s ",
                              "ps",
                              "eps",
                              "aps");
        }
        System.out.printf("   %3s", "err");
        System.out.println();
        for (int test = 1; test <= 23; ++test) {
            System.out.printf("%3d", test);

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
                .stream()//.parallel()
                .map(runner::runElection)
                .collect(listCollector());
            final int totalSeats = results.stream().mapToInt(d -> d.getFinalRound().getSeats()).sum();
            final int totalElected = results.stream().mapToInt(d -> d.getFinalRound().getElected().size()).sum();
            System.out.printf(" %3d %3d %3d %3d %3d  ",
                              elections.size(),
                              elections.stream().mapToInt(Election::getSeats).sum(),
                              results.size(),
                              totalSeats,
                              totalElected);
            var preferredParties = new Counter<Party>();
            var electedParties = JImmutables.<Party>multiset();
            for (ElectionResult district : results) {
                preferredParties = preferredParties.add(district.getPartyFirstChoiceCounts());
                electedParties = electedParties.insertAll(district.getPartyElectedCounts());
            }
            var errors = Decimal.ZERO;
            for (Party party : factory.allParties()) {
                final var actualSeats = electedParties.count(party);
                final var expectedSeats = preferredParties.get(party).dividedBy(preferredParties.getTotal()).times(totalSeats);
                System.out.printf("  %4d   %4s%%  %4s%%",
                                  actualSeats,
                                  percent(preferredParties.get(party), preferredParties.getTotal()),
                                  percent(actualSeats, electedParties.occurrenceCount()));
                final var error = expectedSeats.minus(new Decimal(actualSeats));
                errors = errors.plus(error.squared());
            }
            errors = errors.root();
            System.out.printf("  %4s%%", percent(errors, new Decimal(totalSeats)));

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

    private static BigDecimal percent(int amount,
                                      int maxAmount)
    {
        var numer = new BigDecimal(amount);
        var denom = new BigDecimal(maxAmount);
        var ratio = percent(numer, denom);
        return ratio;
    }

    private static BigDecimal percent(Decimal amount,
                                      Decimal maxAmount)
    {
        var numer = amount.toBigDecimal();
        var denom = maxAmount.toBigDecimal();
        var ratio = percent(numer, denom);
        return ratio;
    }

    private static BigDecimal percent(BigDecimal numer,
                                      BigDecimal denom)
    {
        return numer
            .multiply(HUNDRED)
            .divide(denom, 8, RoundingMode.HALF_UP)
            .setScale(1, RoundingMode.HALF_UP);
    }

    private static String center(String s,
                                 int width)
    {
        s = " " + s + " ";
        while (s.length() < width) {
            s = "-" + s;
            if (s.length() < width) {
                s = s + "-";
            }
        }
        return s;
    }
}
