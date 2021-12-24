package com.burtonzone.parties;

import static com.burtonzone.parties.Party.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.stv.Ballot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Affinity
{
    public static final Affinity Lefts = new Affinity(Left, CenterLeft);
    public static final Affinity Centers = new Affinity(CenterLeft, Center, CenterRight);
    public static final Affinity Rights = new Affinity(CenterRight, Right);
    public static final JImmutableList<Affinity> All = JImmutables.list(Lefts, Centers, Rights);

    @Getter
    private final JImmutableList<Party> parties;

    public Affinity(Party... parties)
    {
        this.parties = JImmutables.list(parties);
    }

    public static Ballot randomAffinityBallot(Random rand,
                                              int numberOfSeats,
                                              JImmutableList<Candidate> candidates)
    {
        final var i1 = rand.nextInt(3 * All.size());
        final var i2 = rand.nextInt(3 * All.size());
        final var i3 = rand.nextInt(3 * All.size());
        final var affinityIndex = (i1 + i2 + i3) / 9;
        final var affinity = All.get(affinityIndex);
        return affinity.randomBallot(rand, numberOfSeats, candidates);
    }

    public Ballot randomBallot(Random rand,
                               int numberOfSeats,
                               JImmutableList<Candidate> candidates)
    {
        final var sortedParties = new ArrayList<>(parties.getList());
        Collections.shuffle(sortedParties, rand);
        final var sortedCandidates = new ArrayList<>(candidates.getList());
        Collections.shuffle(sortedCandidates);
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
}
