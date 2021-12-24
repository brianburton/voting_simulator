package com.burtonzone.stv;

import com.burtonzone.parties.Affinity;
import com.burtonzone.parties.Candidate;
import com.burtonzone.parties.Party;
import java.util.Random;
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

    public static District randomDistrict(Random rand,
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
        for (int b = 1; b <= 1000 * seats; ++b) {
            rb.ballot(Affinity.randomAffinityBallot(rand, seats, candidates));
        }
        final var start = rb.build();
        final var end = start.run();
        return new District(name, start, end);
    }
}
