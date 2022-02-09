package com.burtonzone.election;

import org.javimmutable.collections.JImmutableList;

public interface ElectionFactory
{
    Election createElection(ElectionSettings settings);

    JImmutableList<Party> allParties();
}
