package com.burtonzone.election;

import com.burtonzone.common.Decimal;

public interface PartyPosition
{
    Decimal distanceTo(PartyPosition other);
}
