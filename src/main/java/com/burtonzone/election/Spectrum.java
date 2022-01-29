package com.burtonzone.election;

import static com.burtonzone.election.Party.*;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Rand;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Spectrum
    implements ElectionFactory
{

    private final JImmutableList<Affinity> nodes;
    @Getter
    private final Rand rand;

    public Spectrum(Rand rand)
    {
        this.rand = rand;
        var allAffinities = list(
            new Affinity(Left, CenterLeft),
            new Affinity(CenterLeft, Center),
            new Affinity(Center, CenterRight),
            new Affinity(CenterRight, Right));

        final var nb = JImmutables.<Affinity>listBuilder();
        while (nb.size() < 500) {
            final Affinity affinity = rand.nextElement(allAffinities);
            final int count = randomSize(rand, nb);
            addCount(nb, affinity, count);
        }
        this.nodes = nb.build();
    }

    @Override
    public Election createElection(int numberOfSeats)
    {
        final var cb = JImmutables.<Candidate>listBuilder();
        for (int s = 1; s <= numberOfSeats; ++s) {
            for (Party party : All) {
                cb.add(new Candidate(party, party.getAbbrev() + "-" + s));
            }
        }
        final var candidates = cb.build();

        final var rb = Election.builder();
        rb.seats(numberOfSeats);
        var affinity = nextAffinity();
        for (int b = 1; b <= 1_000 * numberOfSeats; ++b) {
            rb.ballot(affinity.randomBallot(numberOfSeats, candidates));
        }
        return rb.build();
    }

    @Override
    public JImmutableList<Party> allParties()
    {
        return All;
    }

    private int randomSize(Rand rand,
                           JImmutableList.Builder<Affinity> nb)
    {
        final int remaining = Math.max(50, 500 - nb.size());
        return rand.nextIndex(remaining);
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
        private final JImmutableList<Party> parties;

        private Affinity(Party... parties)
        {
            this.parties = list(parties);
        }

        public Ballot randomBallot(int numberOfSeats,
                                   JImmutableList<Candidate> candidates)
        {
            var choices = JImmutables.<Candidate>list();
            var shuffled = rand.shuffle(candidates.getList());
            while (choices.size() < numberOfSeats) {
                var party = rand.nextElement(parties);
                var number = Math.min(numberOfSeats - choices.size(), 1 + rand.nextIndex(numberOfSeats));
                var i = 0;
                while (number > 0 && i < shuffled.size()) {
                    var c = shuffled.get(i);
                    if (c.getParty().equals(party)) {
                        choices = choices.insertLast(c);
                        shuffled = shuffled.delete(i);
                        number -= 1;
                    } else {
                        i += 1;
                    }
                }
            }
            return new Ballot(choices);
        }

        @Override
        public String toString()
        {
            return parties.toString();
        }
    }
}
