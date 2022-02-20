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
     * It can allow a plurality of voters to capture all seats in an election.
     *
     * Included here simply to demonstrate a bad system.
     *
     * Each voter gets one vote per seat and candidates with the highest vote counts are elected.
     */
    public static PluralityRunner blockVote()
    {
        return PluralityRunner.blockVote();
    }

    /**
     * Better than block vote but still allows false majorities.  Just less spectacular than block
     * voting which can give one party with a plurality of the vote to win all seats.
     *
     * Each voter can cast a vote for no more than a majority of the available seats.
     */
    public static PluralityRunner limitedVote()
    {
        return PluralityRunner.limitedVote();
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
