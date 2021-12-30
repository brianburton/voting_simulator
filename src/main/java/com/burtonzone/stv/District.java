package com.burtonzone.stv;

import com.burtonzone.common.Counter;
import com.burtonzone.election.Candidate;
import com.burtonzone.election.Party;
import com.burtonzone.election.Spectrum;
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

    public Counter<Party> getPartyFirstChoiceCounts()
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
        final var cb = JImmutables.<Candidate>listBuilder();
        for (int s = 1; s <= seats; ++s) {
            for (Party party : Party.All) {
                cb.add(new Candidate(party, party.getAbbrev() + "-" + s));
            }
        }
        final var candidates = cb.build();

        final var rb = Round.builder();
        rb.seats(seats);
        var affinity = spectrum.nextAffinity();
        for (int b = 1; b <= 550 * seats; ++b) {
            rb.ballot(affinity.randomBallot(seats, candidates));
        }
        affinity = spectrum.nextAffinity();
        for (int b = 1; b <= 450 * seats; ++b) {
            rb.ballot(affinity.randomBallot(seats, candidates));
        }
        final var start = rb.build();
        final var end = start.run();
        return new District(name, start, end);
    }
}
