package com.burtonzone.model;

import lombok.Value;

@Value
public class Candidate implements Comparable<Candidate>
{
    String name;
    int priority;

    @Override
    public int compareTo(Candidate o)
    {
        var diff = Integer.compare(priority, o.priority);
        if (diff == 0) {
            diff = name.compareTo(o.name);
        }
        return diff;
    }
}
