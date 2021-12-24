package com.burtonzone.stv;

import com.burtonzone.parties.Candidate;
import com.burtonzone.parties.Party;
import com.burtonzone.parties.Spectrum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javimmutable.collections.JImmutableMultiset;
import org.javimmutable.collections.util.JImmutables;

@Getter
@AllArgsConstructor
public class District
{
    private final String name;
    private final Round start;
    private final Round end;

    public JImmutableMultiset<Party> getPartyFirstChoiceCounts()
    {
        return start.getBallotBox().getPartyFirstChoiceCounts();
    }

    public JImmutableMultiset<Party> getPartyElectedCounts()
    {
        return end.getElected().stream()
            .map(Candidate::getParty)
            .collect(JImmutables.multisetCollector());
    }

    public static District randomDistrict(Spectrum spectrum,
                                          String name,
                                          int seats)
    {
        final var affinity = spectrum.nextAffinity();
        final var cb = JImmutables.<Candidate>listBuilder();
        for (int s = 1; s <= seats; ++s) {
            for (Party party : Party.All) {
                cb.add(new Candidate(party, party.getAbbrev() + "-" + s));
            }
        }
        final var candidates = cb.build();

        final var rb = Round.builder();
        rb.seats(seats);
        for (int b = 1; b <= 1000 * seats; ++b) {
            rb.ballot(affinity.randomBallot(seats, candidates));
        }
        final var start = rb.build();
        final var end = start.run();
        return new District(name, start, end);
    }
}
