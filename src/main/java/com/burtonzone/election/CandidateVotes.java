package com.burtonzone.election;

import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;

@Value
@AllArgsConstructor
public class CandidateVotes
{
    public enum SelectionType
    {
        Vote,
        List
    }

    Candidate candidate;
    Decimal votes;
    SelectionType selectionType;

    public CandidateVotes(Counter.Entry<Candidate> e,
                          SelectionType selectionType)
    {
        this(e.getKey(), e.getCount(), selectionType);
    }

    @Override
    public String toString()
    {
        return "" + candidate + "=" + votes;
    }

    public boolean isList()
    {
        return selectionType == SelectionType.List;
    }

    public static JImmutableSet<Candidate> toCandidateSet(JImmutableList<CandidateVotes> elected)
    {
        return elected.transform(set(), CandidateVotes::getCandidate);
    }

    public static Comparator<CandidateVotes> voteOrder(Comparator<Candidate> tieBreaker)
    {
        return Comparator
            .comparing(CandidateVotes::getVotes)
            .reversed()
            .thenComparing(CandidateVotes::getCandidate, tieBreaker)
            .thenComparing(CandidateVotes::getCandidate);
    }

    public static int countType(IterableStreamable<CandidateVotes> list,
                                SelectionType type)
    {
        return (int)list.stream()
            .filter(cv -> cv.getSelectionType() == type)
            .count();
    }
}
