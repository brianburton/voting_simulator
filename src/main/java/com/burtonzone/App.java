package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.Party;
import com.burtonzone.election.Spectrum;
import com.burtonzone.open_list.OpenListRunner;
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
        for (int test = 1; test <= 10; ++test) {
            if (test > 1) {
                System.out.println();
            }
            System.out.printf("Test %d (seed=%d)", test, rand.getSeed());

//            final var runner = new BasicStvRunner();
            final var runner = new OpenListRunner();
            final var spectrum = new Spectrum(rand);
            final var db = JImmutables.<DistrictSpec>listBuilder();
            // number and size of districts taken from fairvote.org plan for US house elections
            addDistricts(db, 44, 5);
            addDistricts(db, 9, 4);
            addDistricts(db, 54, 3);
            addDistricts(db, 5, 2);
            addDistricts(db, 7, 1);
            // double size districts
//            addDistricts(db, 44, 10);
//            addDistricts(db, 9, 8);
//            addDistricts(db, 54, 6);
//            addDistricts(db, 5, 4);
//            addDistricts(db, 7, 2);
            final JImmutableList<Election> elections =
                db.build()
                    .stream().parallel()
                    .map(spec -> spec.create(spectrum))
                    .collect(listCollector());

            System.out.println();
            System.out.println("Election count: " + elections.size());
            System.out.println("Election Seats: " + elections.stream().mapToInt(Election::getSeats).sum());

            final var results = elections
                .stream()//.parallel()
                .map(runner::runElection)
                .collect(listCollector());
            System.out.println("District count: " + results.size());
            System.out.println("Seat count    : " + results.stream().mapToInt(d -> d.getFinalRound().getSeats()).sum());
            System.out.println("Elected count : " + results.stream().mapToInt(d -> d.getFinalRound().getElected().size()).sum());
            System.out.println();
            System.out.println("Party         Seats    Exp    Act");
            var preferredParties = new Counter<Party>();
            for (ElectionResult district : results) {
                preferredParties = preferredParties.add(district.getPartyFirstChoiceCounts());
            }
            var electedParties = JImmutables.<Party>multiset();
            for (ElectionResult district : results) {
                electedParties = electedParties.insertAll(district.getPartyElectedCounts());
            }
            for (Party party : Party.All) {
                System.out.printf("%-12s  %4d   %4s%%  %4s%%%n",
                                  party.getName(),
                                  electedParties.count(party),
                                  percent(preferredParties.get(party), preferredParties.getTotal()),
                                  percent(electedParties.count(party), electedParties.occurrenceCount()));
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

        Election create(Spectrum spectrum)
        {
            return Election.random(spectrum, numberOfSeats);
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
}
