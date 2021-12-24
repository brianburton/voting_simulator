package com.burtonzone.parties;

import static com.burtonzone.parties.Party.*;

import com.burtonzone.common.Decimal;
import com.burtonzone.common.Rand;
import com.burtonzone.stv.Ballot;
import java.util.LinkedHashSet;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Affinity
{
    private static final Affinity Lefts = new Affinity(Left, CenterLeft);
    private static final Affinity Centers = new Affinity(CenterLeft, Center, Center, CenterRight);
    private static final Affinity Rights = new Affinity(CenterRight, Right);
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
}
