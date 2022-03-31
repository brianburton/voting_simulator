package com.burtonzone;

import com.burtonzone.election.DistrictMap;
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
     * Fantasy district map with 14 districts having 10 seats each.
     */
    Fantasy10x14(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 10, 14)
        .build()),

    /**
     * Fantasy district map with 20 districts having 7 seats each.
     */
    Fantasy7x20(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 7, 20)
        .build()),

    /**
     * Fantasy district map with 28 districts having 5 seats each.
     */
    Fantasy5x28(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 5, 28)
        .build()),

    /**
     * Fantasy district map with 28 districts having 5 seats each.
     */
    Fantasy1x140(electionSettings -> DistrictMap.builder()
        .add(electionSettings, 1, 140)
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
