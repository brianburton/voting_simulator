package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import org.javimmutable.collections.JImmutableList;

public interface IssueSpace
{
    JImmutableList<Integer> PartyPoints = list(10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90);
    JImmutableList<Integer> CenterPartyPoints = list(40, 45, 50, 55, 60);

    PartyPosition centristPartyPosition();

    PartyPosition anyPartyPosition();

    PartyPosition voterCenterPosition();

    PartyPosition voterPosition(PartyPosition voterCenterPosition);

    PartyPosition candidatePosition(PartyPosition partyPosition);

    PartyPosition center();
}
