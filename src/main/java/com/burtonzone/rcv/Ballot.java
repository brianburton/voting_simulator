package com.burtonzone.rcv;

import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

@Value
public class Ballot
{
    JImmutableList<Candidate> choices;

    public Ballot(Candidate... choices)
    {
        this.choices = JImmutables.list(choices);
    }

    public Ballot(JImmutableList<Candidate> choices)
    {
        this.choices = choices;
    }

    public boolean isValid()
    {
        return choices.size() > 0 && JImmutables.set(choices).size() == choices.size();
    }

    public Candidate getFirstChoice()
    {
        return choices.get(0);
    }

    public Ballot withoutCandidate(Candidate... candidates)
    {
        final var removed = JImmutables.set(candidates);
        return new Ballot(choices.reject(removed::contains));
    }

    @Override
    public String toString()
    {
        return choices.toString();
    }
}
