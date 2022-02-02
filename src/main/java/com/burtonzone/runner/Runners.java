package com.burtonzone.runner;

import com.burtonzone.runner.basic_stv.BasicStvRunner;

public class Runners
{
    public static BasicStvRunner basicStv()
    {
        return new BasicStvRunner();
    }

    public static SingleVoteRunner singleVote()
    {
        return new SingleVoteRunner();
    }

    public static BlockPluralityRunner blockPlurality()
    {
        return new BlockPluralityRunner();
    }

    public static OpenListHareRunner openListHare()
    {
        return new OpenListHareRunner();
    }

    public static OpenListFormulaRunner dhondt()
    {
        return OpenListFormulaRunner.dhondt();
    }

    public static OpenListFormulaRunner sainteLaguë()
    {
        return OpenListFormulaRunner.sainteLaguë();
    }
}
