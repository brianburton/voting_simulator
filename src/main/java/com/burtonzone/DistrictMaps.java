package com.burtonzone;

import com.burtonzone.election.ElectionSettings;
import java.util.function.Function;

public enum DistrictMaps
{
    /**
     * Current number of single-seat districts as US House of Representatives.
     */
    CongressSingles(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 1, 435)
        .build()),

    /**
     * Number and size of districts taken from fairvote.org plan for US house elections.
     */
    CongressFairVote(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 5, 44)
        .add(electionSettings, 4, 9)
        .add(electionSettings, 3, 54)
        .add(electionSettings, 2, 5)
        .add(electionSettings, 1, 7)
        .build()),

    /**
     * Number and size of districts inspired by fairvote.org plan for US house elections but with district sizes increased.
     */
    CongressFairVoteLarge(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 9, 44)
        .add(electionSettings, 7, 9)
        .add(electionSettings, 5, 54)
        .add(electionSettings, 3, 5)
        .add(electionSettings, 2, 7)
        .build()),

    /**
     * Same number of seats as Maryland House of Delegates but with made up
     * multi-member districts with max of 9 seats per district.
     */
    MarylandDelegatesMax9(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 9, 44)
        .add(electionSettings, 7, 9)
        .add(electionSettings, 5, 54)
        .add(electionSettings, 3, 5)
        .add(electionSettings, 2, 7)
        .build()),

    /**
     * Same number of seats as Maryland House of Delegates but with made up
     * multi-member districts with max of 10 seats per district.
     */
    MarylandDelegatesMax10(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 10, 10)
        .add(electionSettings, 9, 1)
        .add(electionSettings, 8, 4)
        .build());

    private final Function<ElectionSettings, DistrictMap> factory;

    DistrictMaps(Function<ElectionSettings, DistrictMap> factory)
    {
        this.factory = factory;
    }

    public DistrictMap create(ElectionSettings election)
    {
        return factory.apply(election);
    }
}
