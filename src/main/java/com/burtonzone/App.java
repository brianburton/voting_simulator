package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.basic_stv.BasicStvRunner;
import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.Party;
import com.burtonzone.election.Spectrum;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            System.out.println("Test " + test);

            final var runner = new BasicStvRunner();
            final var spectrum = new Spectrum(rand);
            final var db = JImmutables.<Election>listBuilder();
            // number and size of districts taken from fairvote.org plan for US house elections
            addDistricts(spectrum, db, 44, 5, "five-");
            addDistricts(spectrum, db, 9, 4, "four-");
            addDistricts(spectrum, db, 54, 3, "three-");
            addDistricts(spectrum, db, 5, 2, "two-");
            addDistricts(spectrum, db, 7, 1, "one-");
            final JImmutableList<Election> elections = db.build();

            System.out.println();
            System.out.println("Election count: " + elections.size());
            System.out.println("Election Seats: " + elections.stream().mapToInt(Election::getSeats).sum());

            final var results = elections
                .stream().parallel()
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

    private static void addDistricts(Spectrum spectrum,
                                     JImmutableList.Builder<Election> db,
                                     int numberOfDistricts,
                                     int numberOfSeatsPerDistrict,
                                     String namePrefix
    )
    {
        for (int i = 1; i <= numberOfDistricts; ++i) {
            db.add(Election.random(spectrum, numberOfSeatsPerDistrict));
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
