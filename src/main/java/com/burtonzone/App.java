package com.burtonzone;

import com.burtonzone.common.Rand;
import com.burtonzone.parties.Party;
import com.burtonzone.stv.District;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.javimmutable.collections.util.JImmutables;

public class App
{
    public static void main(String[] args)
    {
        final var fives = 20;
        final var threes = 0;
        final var rand = new Rand();
        final var db = JImmutables.<District>listBuilder();
        for (int i = 1; i <= Math.max(fives, threes); ++i) {
            if (i <= fives) {
                db.add(District.randomDistrict(rand, "five-" + i, 5));
            }
            if (i <= threes) {
                db.add(District.randomDistrict(rand, "three-" + i, 3));
            }
        }
        final var districts = db.build();
        var preferredParties = JImmutables.<Party>multiset();
        for (District district : districts) {
            preferredParties = preferredParties.insertAll(district.getPartyFirstChoiceCounts());
        }
        var electedParties = JImmutables.<Party>multiset();
        for (District district : districts) {
            electedParties = electedParties.insertAll(district.getPartyElectedCounts());
        }
        for (Party party : Party.All) {
            System.out.printf("%12s  %4s  %4s%n",
                              party.getName(),
                              percent(preferredParties.count(party), preferredParties.occurrenceCount()),
                              percent(electedParties.count(party), electedParties.occurrenceCount()));
        }
    }

    private static BigDecimal percent(int amount,
                                      int maxAmount)
    {
        var numer = new BigDecimal(amount).setScale(3, RoundingMode.HALF_UP);
        var denom = new BigDecimal(maxAmount).setScale(3, RoundingMode.HALF_UP);
        var ratio = numer
            .multiply(new BigDecimal(100))
            .divide(denom, RoundingMode.HALF_UP)
            .setScale(1, RoundingMode.HALF_UP);
        return ratio;
    }
}
