package com.burtonzone;

import com.burtonzone.election.ElectionRunner;
import com.burtonzone.runner.LargeAndSmallElectionRunner;
import com.burtonzone.runner.OpenListFormulaRunner;
import com.burtonzone.runner.OpenListHareRunner;
import com.burtonzone.runner.PluralityRunner;
import com.burtonzone.runner.basic_stv.BasicStvRunner;

public class Runners
{
    public static BasicStvRunner basicStv()
    {
        return new BasicStvRunner();
    }

    /**
     * Each voter gets one vote per seat and candidates with the highest vote counts are elected.
     */
    public static PluralityRunner singleVote()
    {
        return PluralityRunner.singleVote();
    }

    /**
     * NOT a PR system at all.  In fact this is the opposite since it produces false majorities.
     * It can allow a majority of voters to capture all seats in an election or a plurality to
     * win a majority of the seats.
     *
     * Included here simply to demonstrate a bad system.
     *
     * Each voter gets one vote per seat and candidates with the highest vote counts are elected.
     */
    public static PluralityRunner blockVote()
    {
        return PluralityRunner.blockVote();
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

    public static ElectionRunner hybrid(ElectionRunner largeRunner)
    {
        return new LargeAndSmallElectionRunner(basicStv(), largeRunner, 2);
    }
}
