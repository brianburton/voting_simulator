package com.burtonzone;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.election.Election;
import com.burtonzone.election.ElectionFactory;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.ElectionRunner;
import com.burtonzone.election.ElectionSettings;
import java.util.stream.Stream;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;

public class DistrictMap
{
    private final JImmutableList<DistrictSpec> districts;

    private DistrictMap(JImmutableList<DistrictSpec> districts)
    {
        this.districts = districts;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Elections create(ElectionFactory factory,
                            boolean parallelExecution)
    {
        return new Elections(createImpl(factory, parallelExecution, districts.stream()), parallelExecution);
    }

    public int getSeats()
    {
        return districts.stream().mapToInt(d -> d.getSettings().getNumberOfSeats()).sum();
    }

    private JImmutableList<Election> createImpl(ElectionFactory factory,
                                                boolean parallelExecution,
                                                Stream<DistrictSpec> specs)
    {
        if (parallelExecution) {
            specs = specs.parallel();
        }
        return specs.map(spec -> spec.create(factory))
            .collect(listCollector());
    }

    @Value
    private static class DistrictSpec
    {
        ElectionSettings settings;

        Election create(ElectionFactory factory)
        {
            return factory.createElection(settings);
        }
    }

    @Value
    public static class Elections
    {
        JImmutableList<Election> elections;
        boolean parallel;

        private Elections(JImmutableList<Election> elections,
                          boolean parallel)
        {
            this.elections = elections;
            this.parallel = parallel;
        }

        public Results run(ElectionRunner runner)
        {
            var stream = elections.stream();
            if (parallel) {
                stream = stream.parallel();
            }
            var results = stream
                .map(runner::runElection)
                .collect(listCollector());
            return new Results(this, results, ResultsReport.of(results));
        }
    }

    @Value
    public static class Results
    {
        Elections elections;
        JImmutableList<ElectionResult> results;
        ResultsReport report;
    }

    public static class Builder
    {
        private final JImmutableList.Builder<DistrictSpec> specs = listBuilder();

        public DistrictMap build()
        {
            return new DistrictMap(specs.build());
        }

        public Builder add(ElectionSettings settings,
                           int numberOfSeats,
                           int numberOfDistricts)
        {
            return add(settings.withNumberOfSeats(numberOfSeats), numberOfDistricts);
        }

        public Builder add(ElectionSettings settings,
                           int numberOfDistricts
        )
        {
            final var districtSpec = new DistrictSpec(settings);
            for (int i = 1; i <= numberOfDistricts; ++i) {
                specs.add(districtSpec);
            }
            return this;
        }
    }
}
