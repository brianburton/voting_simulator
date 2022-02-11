package com.burtonzone.election;

import com.burtonzone.common.Rand;

public class IssueSpaces
{
    public static IssueSpace linear(Rand rand)
    {
        return new LinearIssueSpace(rand);
    }

    public static IssueSpace grid(Rand rand)
    {
        return new GridIssueSpace(rand);
    }
}
