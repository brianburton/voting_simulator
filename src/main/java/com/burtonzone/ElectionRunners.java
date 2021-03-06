package com.burtonzone;

import com.burtonzone.election.ElectionRunner;
import com.burtonzone.runner.MmpRunner;
import com.burtonzone.runner.OpenListRunner;
import com.burtonzone.runner.OpenListRunner.Config;
import com.burtonzone.runner.PluralityRunner;
import com.burtonzone.runner.StvRunner;
import java.util.function.Supplier;

public enum ElectionRunners
{
    STV(StvRunner::new),

    /**
     * Each voter gets one vote per seat and candidates with the highest vote counts are elected.
     */
    SingleVote(PluralityRunner::singleVote),

    /**
     * NOT a PR system at all.  In fact this is the opposite since it produces false majorities.
     * It can allow a plurality of voters to capture all seats in an election.
     *
     * Included here simply to demonstrate a bad system.
     *
     * Each voter gets one vote per seat and candidates with the highest vote counts are elected.
     */
    BlockVote(PluralityRunner::blockVote),

    /**
     * Better than block vote but still allows false majorities.  Just less spectacular than block
     * voting which can give one party with a plurality of the vote to win all seats.
     *
     * Each voter can cast a vote for no more than a majority of the available seats.
     */
    LimitedVote(PluralityRunner::limitedVote),
    Hare(() -> new OpenListRunner(Config.builder()
                                      .seatAllocator(Config.PartySeatAllocator.Hare)
                                      .partyVoteMode(Config.PartyVoteMode.Voter)
                                      .listMode(Config.PartyListMode.Party)
                                      // pre-assignment using quotas is not compatible with Hare
                                      .quotasMode(Config.QuotasMode.PartyOnly)
                                      .build())),
    DHondt(() -> new OpenListRunner(Config.builder()
                                        .seatAllocator(Config.PartySeatAllocator.DHondt)
                                        .partyVoteMode(Config.PartyVoteMode.Voter)
                                        .listMode(Config.PartyListMode.Party)
                                        .quotasMode(Config.QuotasMode.TotalAndParty)
                                        .build())),
    Webster(() -> new OpenListRunner(Config.builder()
                                         .seatAllocator(Config.PartySeatAllocator.Webster)
                                         .partyVoteMode(Config.PartyVoteMode.Voter)
                                         .listMode(Config.PartyListMode.Party)
                                         .quotasMode(Config.QuotasMode.TotalAndParty)
                                         .build())),
    MMP_Third(() -> new MmpRunner(seats -> 1 + (4 * seats) / 3)),
    MMP_Half(() -> new MmpRunner(seats -> 1 + (3 * seats) / 2)),
    MMP(() -> new MmpRunner(seats -> 2 * seats));

    private final Supplier<ElectionRunner> factory;

    ElectionRunners(Supplier<ElectionRunner> factory)
    {
        this.factory = factory;
    }

    public ElectionRunner create()
    {
        return factory.get();
    }

    public boolean isMmp()
    {
        return create() instanceof MmpRunner;
    }
}
