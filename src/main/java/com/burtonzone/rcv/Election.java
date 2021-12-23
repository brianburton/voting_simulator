package com.burtonzone.rcv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.util.JImmutables;

public class Election
{
    private final int numOffices;
    private final int minBasisPoints;

    public Election(int numOffices)
    {
        this.numOffices = numOffices;
        this.minBasisPoints = minBasisPointsForNumOffices(numOffices);
    }

    public Round run(Round round)
    {
        while (true) {
            if (isComplete(round)) {
                return round;
            }
            if (!canProceed(round)) {
                return round;
            }
            round = round.advance();
        }
    }

    public JImmutableList<Round.CandidateVotes> getElected(Round round)
    {
        if (isCorrect(round)) {
            return round.getCounts().stream()
                .filter(cv -> basisPoints(cv, round) >= minBasisPoints)
                .collect(JImmutables.listCollector());
        } else {
            return JImmutables.list();
        }
    }

    public boolean isComplete(Round round)
    {
        var count = round.getCounts().stream()
            .filter(cv -> basisPoints(cv, round) >= minBasisPoints)
            .mapToInt(cv -> cv.getCandidates().size())
            .sum();
        return count >= numOffices;
    }

    public boolean isCorrect(Round round)
    {
        var count = round.getCounts().stream()
            .filter(cv -> basisPoints(cv, round) >= minBasisPoints)
            .mapToInt(cv -> cv.getCandidates().size())
            .sum();
        return count == numOffices;
    }

    public boolean canProceed(Round round)
    {
        return round.getCandidateCount() > numOffices && round.getLastPlace().isSingle();
    }

    public static int basisPoints(Round.CandidateVotes cv,
                                  Round round)
    {
        return basisPoints(cv.getVotes(), round.getTotalVotes());
    }

    public static int basisPoints(int votes,
                                  int totalVotes)
    {
        var numer = new BigDecimal(votes).setScale(10, RoundingMode.HALF_UP);
        var denom = new BigDecimal(totalVotes).setScale(10, RoundingMode.HALF_UP);
        var points = numer.divide(denom, 10, RoundingMode.HALF_UP);
        return points.movePointRight(4).intValue();
    }

    public static int minBasisPointsForNumOffices(int numOffices)
    {
        return 1 + basisPoints(1, 1 + numOffices);
    }
}
