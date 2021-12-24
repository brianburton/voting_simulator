package com.burtonzone.parties;

import static com.burtonzone.parties.Party.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.stv.Ballot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

public class Spectrum
{
    private final JImmutableList<Node> nodes;
    @Getter
    private final Rand rand;
    private final int maxValue;

    public Spectrum(Rand rand)
    {
        var nb = JImmutables.<Node>listBuilder();
        nb.add(new Node(new Affinity(Left, CenterLeft), rand.nextIndex(100)));
        nb.add(new Node(new Affinity(CenterRight, Right), rand.nextIndex(100)));
        nb.add(new Node(new Affinity(CenterLeft, Center), rand.nextIndex(100)));
        nb.add(new Node(new Affinity(Center, CenterRight), rand.nextIndex(100)));
        if (rand.nextBoolean()) {
            nb.add(new Node(new Affinity(CenterLeft, Center), 1000));
        } else {
            nb.add(new Node(new Affinity(Center, CenterRight), 1000));
        }
        this.rand = rand;
        this.nodes = nb.build();
        this.maxValue = 400;
    }

    public Affinity nextAffinity()
    {
        var number = rand.nextIndex(maxValue);
        for (Node node : nodes) {
            if (number <= node.threshold) {
                return node.affinity;
            }
            number -= node.threshold;
        }
        return nodes.get(nodes.size() - 1).affinity;
    }

    @AllArgsConstructor
    private static class Node
    {
        Affinity affinity;
        int threshold;
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
            final var sortedCandidates = rand.shuffle(candidates.getList());
            final var choices = sortedCandidates.stream()
                .filter(c -> parties.contains(c.getParty()))
                .limit(numberOfSeats)
                .collect(JImmutables.listCollector());
            return new Ballot(choices, Decimal.ONE);
        }
    }
}
