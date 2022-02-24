package com.burtonzone.election;

import com.burtonzone.common.Rand;
import java.util.function.Function;

public enum IssueSpaces
{
    Linear(LinearIssueSpace::new),
    Grid(GridIssueSpace::new);

    private final Function<Rand, IssueSpace> factory;

    IssueSpaces(Function<Rand, IssueSpace> factory)
    {
        this.factory = factory;
    }

    public IssueSpace create(Rand rand)
    {
        return factory.apply(rand);
    }
}
