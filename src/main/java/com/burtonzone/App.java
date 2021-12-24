package com.burtonzone;

import com.burtonzone.common.Rand;
import com.burtonzone.parties.Affinity;
import com.burtonzone.parties.Party;
import com.burtonzone.stv.District;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.javimmutable.collections.util.JImmutables;

public class App
{
    private static final BigDecimal HUNDRED = new BigDecimal(100).setScale(8, RoundingMode.HALF_UP);

    public static void main(String[] args)
    {
        final var tens = 0;
        final var sevens = 0;
        final var fives = 57;
        final var threes = 50;
        final var rand = new Rand();
        for (int test = 1; test <= 10; ++test) {
            final var spectrum = new Affinity.Spectrum(rand);
            final var db = JImmutables.<District>listBuilder();
            for (int i = 1; i <= tens; ++i) {
                db.add(District.randomDistrict(spectrum, "ten-" + i, 10));
            }
            for (int i = 1; i <= sevens; ++i) {
                db.add(District.randomDistrict(spectrum, "seven-" + i, 7));
            }
            for (int i = 1; i <= fives; ++i) {
                db.add(District.randomDistrict(spectrum, "five-" + i, 5));
            }
            for (int i = 1; i <= threes; ++i) {
                db.add(District.randomDistrict(spectrum, "three-" + i, 3));
            }
            final var districts = db.build();
            if (test > 1) {
                System.out.println();
            }
            System.out.println("Test " + test);
            System.out.println();
            System.out.println("District count: " + districts.size());
            System.out.println("Seat count    : " + districts.stream().mapToInt(d -> d.getEnd().getSeats()).sum());
            System.out.println("Elected count : " + districts.stream().mapToInt(d -> d.getEnd().getElected().size()).sum());
            System.out.println();
            System.out.println("Party         Seats    Exp    Act");
            var preferredParties = JImmutables.<Party>multiset();
            for (District district : districts) {
                preferredParties = preferredParties.insertAll(district.getPartyFirstChoiceCounts());
            }
            var electedParties = JImmutables.<Party>multiset();
            for (District district : districts) {
                electedParties = electedParties.insertAll(district.getPartyElectedCounts());
            }
            for (Party party : Party.All) {
                System.out.printf("%-12s  %4d   %4s%%  %4s%%%n",
                                  party.getName(),
                                  electedParties.count(party),
                                  percent(preferredParties.count(party), preferredParties.occurrenceCount()),
                                  percent(electedParties.count(party), electedParties.occurrenceCount()));
            }
        }
    }

    private static BigDecimal percent(int amount,
                                      int maxAmount)
    {
        var numer = new BigDecimal(amount);
        var denom = new BigDecimal(maxAmount);
        var ratio = numer
            .multiply(HUNDRED)
            .divide(denom, 8, RoundingMode.HALF_UP)
            .setScale(1, RoundingMode.HALF_UP);
        return ratio;
    }
}
