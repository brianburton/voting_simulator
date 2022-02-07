package com.burtonzone.election;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Party
{

    String name;
    String abbrev;
    PartyPosition position;

    @Override
    public String toString()
    {
        return name;
    }
}
