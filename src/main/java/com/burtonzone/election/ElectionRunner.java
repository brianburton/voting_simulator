package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.ResultsReport;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;

public interface ElectionRunner
{
    ElectionResult runElection(Election election);

    default Results runElections(Elections elections)
    {
        var stream = elections.getElections().stream();
        if (elections.isParallel()) {
            stream = stream.parallel();
        }
        var results = stream
            .map(this::runElection)
            .collect(listCollector());
        return new Results(elections, results, ResultsReport.of(results));
    }

    default int getSeatsForMap(DistrictMap districtMap)
    {
        return districtMap.getSeats();
    }

    @Value
    class Elections
    {
        JImmutableList<Election> elections;
        boolean parallel;
    }

    @Value
    class Results
    {
        Elections elections;
        JImmutableList<ElectionResult> results;
        ResultsReport report;
    }
}
