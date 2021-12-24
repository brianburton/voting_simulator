package com.burtonzone.parties;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Party
{
    public static final Party Left = new Party("Left", "L");
    public static final Party CenterLeft = new Party("CenterLeft", "CL");
    public static final Party Center = new Party("Center", "C");
    public static final Party CenterRight = new Party("CenterRight", "CR");
    public static final Party Right = new Party("Right", "R");
    public static final JImmutableList<Party> All = JImmutables.list(Left, CenterLeft, Center, CenterRight, Right);

    String name;
    String abbrev;

    @Override
    public String toString()
    {
        return name;
    }
}
