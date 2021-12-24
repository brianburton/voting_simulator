package com.burtonzone.parties;

import java.util.StringJoiner;
import lombok.Value;

@Value
public class Candidate
    implements Comparable<Candidate>
{
    Party party;
    String name;

    @Override
    public int compareTo(Candidate other)
    {
        return name.compareToIgnoreCase(other.name);
    }

    @Override
    public String toString()
    {
        return new StringJoiner(", ", "[", "]")
            .add(name)
            .add(party.toString())
            .toString();
    }
}
