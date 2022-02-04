package com.burtonzone.runner;

import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LargeAndSmallElectionRunner
    implements ElectionRunner
{
    private final ElectionRunner smallElectionRunner;
    private final ElectionRunner largeElectionRunner;
    private final int largeElectionSeats;

    @Override
    public ElectionResult runElection(Election election)
    {
        if (election.getSeats() >= largeElectionSeats) {
            return largeElectionRunner.runElection(election);
        } else {
            return smallElectionRunner.runElection(election);
        }
    }
}
