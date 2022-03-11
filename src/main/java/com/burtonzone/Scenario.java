package com.burtonzone;

import com.burtonzone.common.Rand;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.ElectionSettings;
import com.burtonzone.election.IssueSpace;
import com.burtonzone.election.IssueSpaces;
import com.burtonzone.election.PositionalElectionFactory;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Scenario
{
    IssueSpace issues;
    ElectionSettings settings;
    ElectionFactory factory;
    ElectionRunner runner;
    DistrictMap districts;

    public static Scenario fromConfig(Config config)
    {
        final var rand = config.hasPath("randomSeed") ? new Rand(config.getLong("randomSeed")) : new Rand();
        final var issueSpace = config.getEnum(IssueSpaces.class, "issueSpace").create(rand);
        final var numParties = config.getInt("numberOfParties");
        final var maxCandidateChoices = config.getInt("maxCandidateChoices");
        final var maxPartyChoices = config.getInt("maxPartyChoices");
        final var mixedPartyVotePercentage = config.getInt("mixedPartyVotePercentage");
        final var voteType = config.getEnum(ElectionSettings.VoteType.class, "voteType");
        final var factory = new PositionalElectionFactory(rand, issueSpace);
        final var parties = factory.createParties(numParties);
        final var electionSettings =
            ElectionSettings.builder()
                .parties(parties)
                .maxCandidateChoices(maxCandidateChoices)
                .maxPartyChoices(maxPartyChoices > 0 ? maxPartyChoices : (1 + numParties) / 2)
                .mixedPartyVotePercentage(mixedPartyVotePercentage)
                .voteType(voteType)
                .build();
        final var electionRunner = config.getEnum(ElectionRunners.class, "electionRunner").create();
        final var districtMap = config.getEnum(DistrictMaps.class, "districtMap").create(electionSettings);
        return builder()
            .issues(issueSpace)
            .settings(electionSettings)
            .factory(factory)
            .runner(electionRunner)
            .districts(districtMap)
            .build();
    }
}
