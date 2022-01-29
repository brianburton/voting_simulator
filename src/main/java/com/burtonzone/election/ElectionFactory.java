package com.burtonzone.election;

import org.javimmutable.collections.JImmutableList;

public interface ElectionFactory
{
    Election createElection(int numberOfSeats);

    JImmutableList<Party> allParties();
}
