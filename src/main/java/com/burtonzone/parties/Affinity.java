package com.burtonzone.parties;

import static com.burtonzone.parties.Party.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.stv.Ballot;
import java.util.LinkedHashSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Affinity
{
    private static final Affinity Lefts = new Affinity(Left, Left, CenterLeft, Center);
    private static final Affinity Centers = new Affinity(CenterLeft, Center, Center, CenterRight);
    private static final Affinity Rights = new Affinity(Center, CenterRight, Right, Right);
    private static final JImmutableList<Affinity> All = JImmutables.list(Lefts, Centers, Centers, Rights);

    @Getter
    private final JImmutableList<Party> parties;

    public Affinity(Party... parties)
    {
        this.parties = JImmutables.list(parties);
    }

    public static Ballot randomAffinityBallot(Rand rand,
                                              int numberOfSeats,
                                              JImmutableList<Candidate> candidates)
    {
        final var affinity = rand.nextElement(All);
        return affinity.randomBallot(rand, numberOfSeats, candidates);
    }

    public Ballot randomBallot(Rand rand,
                               int numberOfSeats,
                               JImmutableList<Candidate> candidates)
    {
        final var sortedParties = rand.shuffle(parties.getList());
        final var sortedCandidates = rand.shuffle(candidates.getList());
        final var choices = new LinkedHashSet<Candidate>();
        for (Party party : sortedParties) {
            for (Candidate candidate : sortedCandidates) {
                if (choices.size() >= numberOfSeats) {
                    break;
                }
                if (candidate.getParty().equals(party)) {
                    choices.add(candidate);
                }
            }
        }
        return new Ballot(JImmutables.list(choices.iterator()), Decimal.ONE);
    }

    public static class Spectrum
    {
        private final JImmutableList<Node> nodes;
        @Getter
        private final Rand rand;
        private final int maxValue;

        public Spectrum(Rand rand)
        {
            var nb = JImmutables.<Node>listBuilder();
            var size = 10 + rand.nextIndex(40);
            nb.add(new Node(Lefts, size));
            var max = size;
            size = 10 + rand.nextIndex(40);
            nb.add(new Node(Rights, size));
            max += size;
            nb.add(new Node(Centers, 100));
            max += 100;
            this.rand = rand;
            this.nodes = nb.build();
            this.maxValue = max;
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
            return Centers;
        }

        public Ballot randomBallot(int numberOfSeats,
                                   JImmutableList<Candidate> candidates)
        {
            final var affinity = nextAffinity();
            return affinity.randomBallot(rand, numberOfSeats, candidates);
        }

        @AllArgsConstructor
        private static class Node
        {
            Affinity affinity;
            int threshold;
        }
    }
}
