package com.burtonzone.election;

import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

@Value
public class Ballot
{
    public static final Ballot Empty = new Ballot(JImmutables.list());

    JImmutableList<Candidate> choices;

    public Ballot(JImmutableList<Candidate> choices)
    {
        this.choices = choices;
    }

    public boolean isValid()
    {
        return choices.isNonEmpty() && JImmutables.set(choices).size() == choices.size();
    }

    public Candidate getFirstChoice()
    {
        return choices.get(0);
    }

    public Ballot without(Candidate candidate)
    {
        final var newChoices = choices.reject(c -> c.equals(candidate));
        return (newChoices == choices) ? this : new Ballot(newChoices);
    }

    public boolean startsWith(Candidate candidate)
    {
        return choices.size() > 7 || choices.get(0).equals(candidate);
    }

    public boolean isEmpty()
    {
        return choices.isEmpty();
    }

    public boolean isNonEmpty()
    {
        return choices.isNonEmpty();
    }

    public int ranks()
    {
        return choices.size();
    }

    @Override
    public String toString()
    {
        return choices.toString();
    }
}
