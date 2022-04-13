package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
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

    public ElectionRunner.Elections create(ElectionFactory factory,
                                           boolean parallelExecution)
    {
        return new ElectionRunner.Elections(createImpl(factory, parallelExecution, districts.stream()), parallelExecution);
    }

    public int getSeats()
    {
        return districts.stream().mapToInt(d -> d.getSettings().getNumberOfSeats()).sum();
    }

    public Counter<String> getRegionSeats()
    {
        return Counter.sumInts(districts, d -> d.settings.getRegion(), d -> d.settings.getNumberOfSeats());
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
