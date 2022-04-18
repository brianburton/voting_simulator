package com.burtonzone.election;

import com.burtonzone.common.Counter;
import java.util.function.Predicate;
import lombok.Data;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;

@Data
public class Ballot
{
    private final JImmutableList<Candidate> candidates;
    private final Party party;

    public boolean isEmpty()
    {
        return candidates.isEmpty();
    }

    public Candidate first()
    {
        return candidates.get(0);
    }

    public int size()
    {
        return candidates.size();
    }

    public boolean partyIn(JImmutableSet<Party> parties)
    {
        return parties.contains(party);
    }

    public boolean anyCandidateIn(JImmutableSet<Candidate> candidates)
    {
        return this.candidates.anyMatch(candidates::contains);
    }

    public boolean isWasted(JImmutableSet<Candidate> electedCandidates,
                            JImmutableSet<Party> electedParties)
    {
        return !(partyIn(electedParties) || anyCandidateIn(electedCandidates));
    }

    public boolean isWasted(JImmutableSet<Candidate> electedCandidates)
    {
        return !anyCandidateIn(electedCandidates);
    }

    public boolean isFirst(Candidate candidate)
    {
        return candidates.get(0).equals(candidate);
    }

    public boolean isPrefixMatch(int prefixLength,
                                 Predicate<Candidate> matcher)
    {
        return candidates.stream().limit(prefixLength).anyMatch(matcher);
    }

    public Ballot without(Candidate candidate)
    {
        var newCandidates = candidates.reject(c -> c.equals(candidate));
        return newCandidates == candidates ? this : new Ballot(newCandidates, party);
    }

    public Ballot toPartyVoteFromFirstChoice()
    {
        final Party newParty = first().getParty();
        return party.equals(newParty) ? this : new Ballot(candidates, newParty);
    }

    public Ballot prefix(int length)
    {
        var newCandidates = candidates.slice(0, length);
        return newCandidates == candidates ? this : new Ballot(newCandidates, party);
    }

    public Ballot reject(Predicate<Candidate> criteria)
    {
        var newCandidates = candidates.reject(criteria);
        return newCandidates == candidates ? this : new Ballot(newCandidates, party);
    }

    public Ballot select(Predicate<Candidate> criteria)
    {
        var newCandidates = candidates.select(criteria);
        return newCandidates == candidates ? this : new Ballot(newCandidates, party);
    }

    public Counter<Party> countPartyVotes()
    {
        return Counter.count(candidates, Candidate::getParty).toRatio();
    }
}
