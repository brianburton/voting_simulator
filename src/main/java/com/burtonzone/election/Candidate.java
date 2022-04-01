package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableListMap;

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

    public static JImmutableListMap<Party, Candidate> createPartyLists(JImmutableList<Party> parties,
                                                                       JImmutableList<Candidate> candidates)
    {
        JImmutableListMap<Party, Candidate> answer = listMap();
        for (Party party : parties) {
            var sorted = candidates.stream()
                .filter(c -> c.getParty().equals(party))
                .sorted(distanceComparator(party.getPosition()))
                .collect(listCollector());
            answer = answer.assign(party, sorted);
        }
        return answer;
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
