package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import com.burtonzone.parties.Party;
import com.burtonzone.parties.Spectrum;
import com.burtonzone.stv.District;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class App
{
    private static final BigDecimal HUNDRED = new BigDecimal(100).setScale(8, RoundingMode.HALF_UP);

    public static void main(String[] args)
    {
        final var rand = new Rand();
        for (int test = 1; test <= 10; ++test) {
            final var spectrum = new Spectrum(rand);
            final var db = JImmutables.<DistrictSpec>listBuilder();
            // number and size of districts taken from fairvote.org plan for US house elections
            addDistricts(spectrum, db, 44, 5, "five-");
            addDistricts(spectrum, db, 9, 4, "four-");
            addDistricts(spectrum, db, 54, 3, "three-");
            addDistricts(spectrum, db, 5, 2, "two-");
            addDistricts(spectrum, db, 7, 1, "one-");
            final var districts = db.build()
                .parallelStream()
                .map(DistrictSpec::build)
                .collect(listCollector());
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

    private static void addDistricts(Spectrum spectrum,
                                     JImmutableList.Builder<DistrictSpec> db,
                                     int numberOfDistricts,
                                     int numberOfSeatsPerDistrict,
                                     String namePrefix
    )
    {
        for (int i = 1; i <= numberOfDistricts; ++i) {
            db.add(new DistrictSpec(namePrefix + i, spectrum, numberOfSeatsPerDistrict));
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

    @Getter
    @AllArgsConstructor
    private static class DistrictSpec
    {
        private final String name;
        private final Spectrum spectrum;
        private final int seats;

        public District build()
        {
            return District.randomDistrict(spectrum, name, seats);
        }
    }
}
