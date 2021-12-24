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
        return JImmutables.set(choices).size() == choices.size()
               && weight.isGreaterOrEqualTo(Decimal.ZERO);
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

    public Ballot without(Candidate candidate)
    {
        final var newChoices = choices.reject(c -> c.equals(candidate));
        return (newChoices == choices) ? this : new Ballot(newChoices, weight);
    }

    public boolean startsWith(Candidate candidate)
    {
        return choices.size() > 7 || choices.get(0).equals(candidate);
    }

    public boolean isEmpty()
    {
        return choices.size() == 0 || weight.equals(Decimal.ZERO);
    }

    public int ranks()
    {
        return choices.size();
    }

    public int candidateIndex(Candidate candidate)
    {
//        int index = 0;
//        for (Candidate choice : choices) {
//            if (choice.equals(candidate)) {
//                return index;
//            }
//        }
        return -1;
    }

    @Override
    public String toString()
    {
        return choices.toString() + "=" + weight;
    }
}
