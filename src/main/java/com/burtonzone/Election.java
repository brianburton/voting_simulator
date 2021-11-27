package com.burtonzone;

import com.burtonzone.model.Ballot;
import com.burtonzone.model.Candidate;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMultiset;
import org.javimmutable.collections.util.JImmutables;

public class Election
{
    public Candidate resolveBallots(JImmutableList<Ballot> ballots)
    {
        var candidates = ballots.stream()
            .flatMap(b -> b.getChoices().stream())
            .collect(JImmutables.setCollector());
        var combinations = ballots.stream()
            .collect(JImmutables.multisetCollector());
        return null;
    }

}
