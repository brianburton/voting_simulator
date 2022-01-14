package com.burtonzone.election;

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
        return String.format("%s (%s)", name, party.getAbbrev());
    }
}