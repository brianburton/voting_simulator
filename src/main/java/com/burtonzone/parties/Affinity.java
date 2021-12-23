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
    public static Affinity Lefts = new Affinity(Left, CenterLeft);
    public static Affinity Centers = new Affinity(CenterLeft, Center, CenterRight);
    public static Affinity Rights = new Affinity(CenterRight, Right);

    @Getter
    private final JImmutableList<Party> parties;

    public Affinity(Party... parties)
    {
        this.parties = JImmutables.list(parties);
    }

    public Ballot randomBallot(Random rand,
                               int numberOfSeats,
                               JImmutableList<Candidate> candidates)
    {
        var sortedParties = new ArrayList<>(parties.getList());
        Collections.shuffle(sortedParties, rand);
        var sortedCandidates = new ArrayList<>(candidates.getList());
        Collections.shuffle(sortedCandidates);
        var choices = new LinkedHashSet<Candidate>();
        for (Party party : sortedParties) {
            for (Candidate candidate : sortedCandidates) {
                if (choices.size() < numberOfSeats) {
                    if (candidate.getParty().equals(party)) {
                        choices.add(candidate);
                    }
                }
            }
        }
        return new Ballot(JImmutables.list(choices.iterator()), Decimal.ONE);
    }
}
