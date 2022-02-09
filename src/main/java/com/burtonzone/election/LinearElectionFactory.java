package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class LinearElectionFactory
    implements ElectionFactory
{
    public static final Party Left = new Party("Left", "L", new Position(10));
    public static final Party CenterLeft = new Party("CenterLeft", "CL", new Position(40));
    public static final Party Center = new Party("Center", "C", new Position(50));
    public static final Party CenterRight = new Party("CenterRight", "CR", new Position(60));
    public static final Party Right = new Party("Right", "R", new Position(90));
    public static final JImmutableList<Party> All = list(Left, CenterLeft, Center, CenterRight, Right);

    @Getter
    private final Rand rand;

    public LinearElectionFactory(Rand rand)
    {
        this.rand = rand;
    }

    @Override
    public Election createElection(ElectionSettings settings)
    {
        assert settings.getVoteType() == ElectionSettings.VoteType.Candidate;
        final int numberOfSeats = settings.getNumberOfSeats();
        final var allAffinities = list(
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
        var nodes = nb.build();
        final var cb = JImmutables.<Candidate>listBuilder();
        for (int s = 1; s <= numberOfSeats; ++s) {
            for (Party party : All) {
                cb.add(new Candidate(party, party.getAbbrev() + "-" + s));
            }
        }
        final var candidates = cb.build();

        final var rb = Election.builder();
        rb.parties(All);
        rb.seats(numberOfSeats);
        var affinity = nextAffinity(nodes);
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

    public Affinity nextAffinity(JImmutableList<Affinity> nodes)
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

        public JImmutableList<Candidate> randomBallot(int numberOfSeats,
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
            return choices;
        }

        @Override
        public String toString()
        {
            return parties.toString();
        }
    }

    public static class Position
        implements PartyPosition
    {
        private final Decimal position;

        private Position(int position)
        {
            this.position = new Decimal(position);
        }

        @Override
        public Decimal distanceTo(PartyPosition other)
        {
            return position.minus(((Position)other).position).abs();
        }
    }
}
