package com.burtonzone.election;

import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Candidate
    implements Comparable<Candidate>
{
    Party party;
    String name;
    Position position;

    public Candidate(Party party,
                     String name)
    {
        this(party, name, party.getPosition());
    }

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

    public static Comparator<Candidate> distanceComparator(Position position)
    {
        return new DistanceComparator<>(position, Candidate::getPosition);
    }
}
