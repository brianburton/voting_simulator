package com.burtonzone;

import com.burtonzone.election.ElectionSettings;

public class DistrictMaps
{
    public static DistrictMap congressSingles(ElectionSettings electionSettings)
    {
        return DistrictMap.builder()
            .add(electionSettings, 1, 435)
            .build();
    }

    // number and size of districts taken from fairvote.org plan for US house elections
    public static DistrictMap congressFairVote(ElectionSettings electionSettings)
    {
        return DistrictMap.builder()
            .add(electionSettings, 5, 44)
            .add(electionSettings, 4, 9)
            .add(electionSettings, 3, 54)
            .add(electionSettings, 2, 5)
            .add(electionSettings, 1, 7)
            .build();
    }

    // double number and size of districts taken from fairvote.org plan for US house elections
    public static DistrictMap congressFairVote2x(ElectionSettings electionSettings)
    {
        return DistrictMap.builder()
            .add(electionSettings, 9, 44)
            .add(electionSettings, 7, 9)
            .add(electionSettings, 5, 54)
            .add(electionSettings, 3, 5)
            .add(electionSettings, 2, 7)
            .build();
    }

    public static DistrictMap marylandDelegatesMax7(ElectionSettings electionSettings)
    {
//        final var totalSeats = 141;
        return DistrictMap.builder()
            .add(electionSettings, 7, 16)
            .add(electionSettings, 5, 4)
            .add(electionSettings, 3, 3)
            .build();
    }
}
