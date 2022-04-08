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
        .add(electionSettings.withRegion(""), 9, 44)
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
        .build()),

    CongressStates(electionSettings -> DistrictMap.builder()
        .add(electionSettings.withRegion("Alabama"), 7)
        .add(electionSettings.withRegion("Alaska"), 1)
        .add(electionSettings.withRegion("Arizona"), 9)
        .add(electionSettings.withRegion("Arkansas"), 4)
        .add(electionSettings.withRegion("California"), 53)
        .add(electionSettings.withRegion("Colorado"), 7)
        .add(electionSettings.withRegion("Connecticut"), 5)
        .add(electionSettings.withRegion("Delaware"), 1)
        .add(electionSettings.withRegion("Florida"), 27)
        .add(electionSettings.withRegion("Georgia"), 14)
        .add(electionSettings.withRegion("Hawaii"), 2)
        .add(electionSettings.withRegion("Idaho"), 2)
        .add(electionSettings.withRegion("Illinois"), 18)
        .add(electionSettings.withRegion("Indiana"), 9)
        .add(electionSettings.withRegion("Iowa"), 4)
        .add(electionSettings.withRegion("Kansas"), 4)
        .add(electionSettings.withRegion("Kentucky"), 6)
        .add(electionSettings.withRegion("Louisiana"), 6)
        .add(electionSettings.withRegion("Maine"), 2)
        .add(electionSettings.withRegion("Maryland"), 8)
        .add(electionSettings.withRegion("Massachusetts"), 9)
        .add(electionSettings.withRegion("Michigan"), 14)
        .add(electionSettings.withRegion("Minnesota"), 8)
        .add(electionSettings.withRegion("Mississippi"), 4)
        .add(electionSettings.withRegion("Missouri"), 8)
        .add(electionSettings.withRegion("Montana"), 1)
        .add(electionSettings.withRegion("Nebraska"), 3)
        .add(electionSettings.withRegion("Nevada"), 4)
        .add(electionSettings.withRegion("New Hampshire"), 2)
        .add(electionSettings.withRegion("New Jersey"), 12)
        .add(electionSettings.withRegion("New Mexico"), 3)
        .add(electionSettings.withRegion("New York"), 27)
        .add(electionSettings.withRegion("North Carolina"), 13)
        .add(electionSettings.withRegion("North Dakota"), 1)
        .add(electionSettings.withRegion("Ohio"), 16)
        .add(electionSettings.withRegion("Oklahoma"), 5)
        .add(electionSettings.withRegion("Oregon"), 5)
        .add(electionSettings.withRegion("Pennsylvania"), 18)
        .add(electionSettings.withRegion("Rhode Island"), 2)
        .add(electionSettings.withRegion("South Carolina"), 7)
        .add(electionSettings.withRegion("South Dakota"), 1)
        .add(electionSettings.withRegion("Tennessee"), 9)
        .add(electionSettings.withRegion("Texas"), 36)
        .add(electionSettings.withRegion("Utah"), 4)
        .add(electionSettings.withRegion("Vermont"), 1)
        .add(electionSettings.withRegion("Virginia"), 11)
        .add(electionSettings.withRegion("Washington"), 10)
        .add(electionSettings.withRegion("West Virginia"), 3)
        .add(electionSettings.withRegion("Wisconsin"), 8)
        .add(electionSettings.withRegion("Wyoming"), 1)
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
