package com.burtonzone.rcv.model;

import lombok.Value;

@Value
public class Candidate
    implements Comparable<Candidate>
{
    String name;

    @Override
    public int compareTo(Candidate o)
    {
        return name.compareTo(o.name);
    }


    @Override
    public String toString()
    {
        return name;
    }
}
