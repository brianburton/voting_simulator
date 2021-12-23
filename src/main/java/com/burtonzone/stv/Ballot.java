package com.burtonzone.stv;

import com.burtonzone.common.Decimal;
import com.burtonzone.parties.Candidate;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

@Value
public class Ballot
{
    JImmutableList<Candidate> choices;
    Decimal weight;

    public Ballot(JImmutableList<Candidate> choices,
                  Decimal weight)
    {
        this.choices = choices;
        this.weight = weight;
        assert isValid();
    }

    public boolean isValid()
    {
        return choices.size() > 0
               && JImmutables.set(choices).size() == choices.size()
               && weight.isGreaterThan(Decimal.ZERO);
    }

    public Candidate getFirstChoice()
    {
        return choices.get(0);
    }

    public Ballot weighted(Decimal extraWeight)
    {
        assert extraWeight.isLessThan(Decimal.ONE);
        return new Ballot(choices, weight.times(extraWeight));
    }

    public Ballot without(Candidate... candidates)
    {
        final var removed = JImmutables.set(candidates);
        final var newChoices = choices.reject(removed::contains);
        return (newChoices == choices) ? this : new Ballot(newChoices, weight);
    }

    @Override
    public String toString()
    {
        return choices.toString() + "=" + weight;
    }
}
