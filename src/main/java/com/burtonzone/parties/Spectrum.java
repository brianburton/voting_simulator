package com.burtonzone.parties;

import static com.burtonzone.parties.Party.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.stv.Ballot;
import java.util.stream.IntStream;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

public class Spectrum
{
    private final JImmutableList<Affinity> nodes;
    @Getter
    private final Rand rand;

    public Spectrum(Rand rand)
    {
        final var nb = JImmutables.<Affinity>listBuilder();
        addCount(nb, new Affinity(Left, CenterLeft), randomSize(rand, nb));
        addCount(nb, new Affinity(Right, CenterRight), randomSize(rand, nb));
        addCount(nb, new Affinity(Center, CenterLeft), randomSize(rand, nb));
        addCount(nb, new Affinity(Center, CenterRight), randomSize(rand, nb));
        this.rand = rand;
        this.nodes = nb.build();
    }

    private int randomSize(Rand rand,
                           JImmutableList.Builder<Affinity> nb)
    {
        final var remainder = Math.max(10, 400 - nb.size());
        return rand.nextIndex(Math.min(100, remainder));
    }

    private void addCount(JImmutableList.Builder<Affinity> nb,
                          Affinity affinity,
                          int count)
    {
        for (int i = 0; i < count; ++i) {
            nb.add(affinity);
        }
    }

    public Affinity nextAffinity()
    {
        return rand.nextElement(nodes);
    }

    public class Affinity
    {
        @Getter
        private final JImmutableSet<Party> parties;

        private Affinity(Party... parties)
        {
            this.parties = JImmutables.set(parties);
        }

        public Ballot randomBallot(int numberOfSeats,
                                   JImmutableList<Candidate> candidates)
        {
            final var choices =
                IntStream.range(0, Integer.MAX_VALUE)
                    .mapToObj(i -> rand.nextElement(candidates))
                    .filter(c -> parties.contains(c.getParty()))
                    .distinct()
                    .limit(numberOfSeats)
                    .collect(JImmutables.listCollector());
            return new Ballot(choices, Decimal.ONE);
        }
    }
}
