package com.burtonzone.parties;

import lombok.Value;

@Value
public class Party
{
    public static Party Left = new Party("Left");
    public static Party CenterLeft = new Party("CenterLeft");
    public static Party Center = new Party("Center");
    public static Party CenterRight = new Party("CenterRight");
    public static Party Right = new Party("Right");

    String name;
}
