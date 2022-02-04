package com.burtonzone;

import com.burtonzone.runner.BlockPluralityRunner;
import com.burtonzone.runner.OpenListFormulaRunner;
import com.burtonzone.runner.OpenListHareRunner;
import com.burtonzone.runner.SingleVoteRunner;
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

    public static OpenListHareRunner hare()
    {
        return new OpenListHareRunner();
    }

    public static OpenListFormulaRunner dhondt()
    {
        return new OpenListFormulaRunner(OpenListFormulaRunner.DHondtFormula);
    }

    public static OpenListFormulaRunner webster()
    {
        return new OpenListFormulaRunner(OpenListFormulaRunner.websterFormula);
    }
}
