package com.burtonzone;

import static com.burtonzone.common.Decimal.ZERO;
import static java.lang.String.format;
import static org.javimmutable.collections.util.JImmutables.*;

import com.burtonzone.common.Averager;
import com.burtonzone.common.Counter;
import com.burtonzone.common.DataUtils;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.BallotBox;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.Party;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import lombok.Builder;
import lombok.Value;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

@Value
@Builder
public class ResultsReport
{
    private static final BigDecimal HUNDRED = new BigDecimal(100).setScale(8, RoundingMode.HALF_UP);

    @Builder.Default
    JImmutableSet<Party> parties = JImmutables.insertOrderSet();
    int seats;
    int elected;
    int wasted;
    int votes;
    Party winningParty;
    @Builder.Default
    Decimal effectiveVoteScore = ZERO;
    @Builder.Default
    Decimal averageEffectiveVoteScore = ZERO;
    @Builder.Default
    Decimal averageError = ZERO;
    @Builder.Default
    Decimal averageWasted = ZERO;
    @Builder.Default
    Counter<Party> partyVotes = new Counter<>();
    @Builder.Default
    Counter<Party> partySeats = new Counter<>();
    @Builder.Default
    BallotBox allBallots = BallotBox.builder().build();

    public static ResultsReport of(ElectionResult result)
    {
        return builder()
            .parties(JImmutables.insertOrderSet(result.getElection().getParties()))
            .seats(result.getElection().getSeats())
            .elected(result.getFinalRound().getElected().size())
            .votes(result.getElection().getTotalVotes().toInt())
            .wasted(result.getWasted().toInt())
            .averageEffectiveVoteScore(result.getEffectiveVoteScore())
            .effectiveVoteScore(result.getEffectiveVoteScore())
            .averageError(result.computeErrors())
            .partyVotes(result.getPartyFirstChoiceCounts())
            .partySeats(result.getPartyElectedCounts())
            .winningParty(computeWinningParty(result.getPartyElectedCounts()))
            .allBallots(result.getEffectiveBallots())
            .build();
    }

    public static ResultsReport of(Iterable<ElectionResult> results)
    {
        final var averageError = new Averager();
        final var averageWasted = new Averager();
        final var averageEffectiveVoteScore = new Averager();
        var parties = JImmutables.<Party>insertOrderSet();
        int seats = 0;
        int elected = 0;
        int wasted = 0;
        int votes = 0;
        var partyVotes = new Counter<Party>();
        var partySeats = new Counter<Party>();
        var allBallots = BallotBox.builder();
        var partyElectedCounts = new Counter<Party>();
        var effectiveVoteScore = ZERO;
        for (ElectionResult result : results) {
            parties = parties.insertAll(result.getElection().getParties());
            seats = seats + result.getElection().getSeats();
            elected = elected + result.getFinalRound().getElected().size();
            final Decimal electionTotalVotes = result.getElection().getTotalVotes();
            votes = votes + electionTotalVotes.toInt();
            wasted = wasted + result.getWasted().toInt();
            averageWasted.add(result.getWasted().dividedBy(electionTotalVotes));
            effectiveVoteScore = effectiveVoteScore.plus(result.getEffectiveVoteScore());
            averageEffectiveVoteScore.add(result.getEffectiveVoteScore().dividedBy(electionTotalVotes));
            averageError.add(result.computeErrors());
            partyVotes = partyVotes.add(result.getPartyFirstChoiceCounts());
            partySeats = partySeats.add(result.getPartyElectedCounts());
            partyElectedCounts = partyElectedCounts.add(result.getPartyElectedCounts());
            allBallots.add(result.getEffectiveBallots());
        }
        return builder()
            .parties(parties)
            .seats(seats)
            .elected(elected)
            .votes(votes)
            .wasted(wasted)
            .effectiveVoteScore(effectiveVoteScore)
            .averageEffectiveVoteScore(averageEffectiveVoteScore.average())
            .averageError(averageError.average())
            .averageWasted(averageWasted.average())
            .partyVotes(partyVotes)
            .partySeats(partySeats)
            .winningParty(computeWinningParty(partyElectedCounts))
            .allBallots(allBallots.build())
            .build();
    }

    public int getMajority()
    {
        return seats / 2 + 1;
    }

    public Decimal getAverageNumberOfChoices()
    {
        return allBallots.getAverageNumberOfChoices();
    }

    // https://en.wikipedia.org/wiki/Gallagher_index
    private Decimal computeErrors()
    {
        final var totalSeats = partySeats.getTotal();
        final var totalVotes = partyVotes.getTotal();
        var sum = ZERO;
        for (Party party : parties) {
            final var seatPercentage = partySeats.get(party).dividedBy(totalSeats);
            final var votePercentage = partyVotes.get(party).dividedBy(totalVotes);
            final var diffSquared = votePercentage.minus(seatPercentage).squared();
            sum = sum.plus(diffSquared);
        }
        return sum.dividedBy(Decimal.TWO).root();
    }

    public static String printHeader1(Iterable<Party> parties)
    {
        StringWriter str = new StringWriter();
        try (PrintWriter out = new PrintWriter(str)) {
            out.printf("%-3s %-3s ", "", "");
            for (Party party : parties) {
                out.printf(" %s ", center(party.getName(), 15));
            }
        }
        return str.toString();
    }

    public static String printHeader2(Iterable<Party> parties)
    {
        StringWriter str = new StringWriter();
        try (PrintWriter out = new PrintWriter(str)) {
            out.printf("%3s %3s ", "rsc", "rec");
            for (Party party : parties) {
                out.printf("%8s  %6s ", "eps", "aps");
            }
            out.printf(" %5s %6s %6s %6s", "ranks", "waste", "err", "eff");
        }
        return str.toString();
    }

    public String getRow1()
    {
        StringWriter str = new StringWriter();
        try (PrintWriter out = new PrintWriter(str)) {
            out.printf("%3d %3d", seats, elected);
            for (Party party : parties) {
                final var pr = new PartyResult(party);
                out.printf(" %7s%%  %5s%%", pr.getVotePercent(), pr.getSeatPercent());
            }
            out.printf("  %5s %5s%% %5s%% %5s%%",
                       tenths(getAverageNumberOfChoices()),
                       percent(wasted, votes),
                       percent(computeErrors(), Decimal.ONE),
                       percent(effectiveVoteScore, new Decimal(votes)));
        }
        return str.toString();
    }

    public String getRow2()
    {
        StringWriter str = new StringWriter();
        try (PrintWriter out = new PrintWriter(str)) {
            out.printf("%3s %3s", "", "");
            for (Party party : parties) {
                final var pr = new PartyResult(party);
                out.printf(" %8d  %6d", pr.getExpectedSeats(), pr.getSeats());
            }
            out.printf("  %5s %5s%% %5s%% %5s%%",
                       "",
                       tenths(averageWasted.times(Decimal.HUNDRED)),
                       percent(averageError, Decimal.ONE),
                       tenths(averageEffectiveVoteScore.times(Decimal.HUNDRED)));
        }
        return str.toString();
    }

    public JImmutableList<String> getPartyDistanceGrid()
    {
        final JImmutableList.Builder<String> answer = listBuilder();
        StringBuilder sb = new StringBuilder();
        sb.append(format("%10s", ""));
        for (Party party : parties) {
            sb.append(format("  %10s", party.getName()));
        }
        answer.add(sb.toString());
        for (Party outer : parties) {
            sb = new StringBuilder();
            sb.append(format("%10s", outer.getName()));
            for (Party inner : parties) {
                var distance = outer.getPosition().distanceTo(inner.getPosition()).toInt();
                sb.append(format("  %10d", distance));
            }
            answer.add(sb.toString());
        }
        return answer.build();
    }

    public JImmutableList<String> getCoalitionGrid(int maxDistance)
    {
        final var allCoalitions =
            getCoalitions(maxDistance)
                .stream()
                .sorted(Comparator
                            .comparing(Coalition::getPartyDistance)
                            .thenComparing(Coalition::getSeatPercent))
                .collect(listCollector());
        if (allCoalitions.isEmpty()) {
            return list("  NO WORKING COALITIONS FOUND");
        }

        var previousMembers = JImmutables.<JImmutableSet<Party>>list();
        var filtered = JImmutables.<Coalition>list();
        for (Coalition coalition : allCoalitions) {
            var redundant = false;
            var members = set(coalition.members);
            for (var pm : previousMembers) {
                var shared = pm.intersection(members);
                if (shared.equals(pm)) {
                    redundant = true;
                }
            }
            if (!redundant) {
                filtered = filtered.insertLast(coalition);
                previousMembers = previousMembers.insert(members);
            }
        }

        final JImmutableList.Builder<String> answer = listBuilder();
        answer.add(format("%s %4s %6s %6s %5s  %s", "W", "Dist", "Vote%", "Seat%", "Seats", "Parties"));
        for (Coalition coalition : filtered) {
            answer.add(format("%s %4d %5s%% %5s%% %5d %s",
                              containsWinner(coalition.getMembers()) ? "*" : " ",
                              coalition.getPartyDistance().toInt(),
                              coalition.getVotePercent(),
                              coalition.getSeatPercent(),
                              coalition.getSeats(),
                              coalition.getMembers().transform(Party::getName)));
        }
        return answer.build();
    }

    public <T> JImmutableList<Coalition> getCoalitions(int maxDistance)
    {
        var winner = new PartyResult(winningParty);
        if (winner.hasMajority()) {
            return list(new Coalition(ZERO, list(winningParty), winner.getSeats(), winner.getVotes()));
        }
        final var maxDistanceDecimal = new Decimal(maxDistance);
        var answer = JImmutables.<Coalition>list();
        final var allParties = parties
            .stream()
            .map(PartyResult::new)
            .collect(listCollector());
        for (int count = 1; count <= (parties.size() + 1) / 2; ++count) {
            for (var combo : DataUtils.combos(allParties, count)) {
                var members = JImmutables.<Party>list();
                int seats = 0;
                int votes = 0;
                for (PartyResult member : combo) {
                    members = members.insertLast(member.party);
                    seats = seats + member.getSeats();
                    votes = votes + member.getVotes();
                }
                members = members.stream()
                    .sorted(partySeats.keysHighestFirstOrder())
                    .collect(listCollector());
                final Decimal partyDistance = Party.maxInterPartyDistance(members);
                final Coalition coalition = new Coalition(partyDistance, members, seats, votes);
                if (coalition.getSeatPercent().intValue() <= 50) {
                    continue;
                }
                if (partyDistance.isGreaterThan(maxDistanceDecimal)) {
                    continue;
                }
                answer = answer.insertLast(coalition);
            }
        }
        return answer;
    }

    private boolean containsWinner(JImmutableList<Party> parties)
    {
        return parties.anyMatch(p -> p == winningParty);
    }

    public class PartyResult
    {
        private final Party party;

        private PartyResult(Party party)
        {
            this.party = party;
        }

        public int getVotes()
        {
            return partyVotes.get(party).toInt();
        }

        public int getSeats()
        {
            return partySeats.get(party).toInt();
        }

        public int getExpectedSeats()
        {
            final var ourVotes = partyVotes.get(party);
            final var totalVotes = partyVotes.getTotal();
            final var totalSeats = new Decimal(elected);
            return ourVotes
                .dividedBy(totalVotes)
                .times(totalSeats)
                .toInt();
        }

        public BigDecimal getVotePercent()
        {
            return percent(getVotes(), ResultsReport.this.votes);
        }

        public BigDecimal getSeatPercent()
        {
            return percent(getSeats(), seats);
        }

        public boolean hasMajority()
        {
            return getSeats() >= getMajority();
        }
    }

    @Value
    public class Coalition
    {
        Decimal partyDistance;
        JImmutableList<Party> members;
        int seats;
        int votes;

        public BigDecimal getVotePercent()
        {
            return percent(votes, ResultsReport.this.votes);
        }

        public BigDecimal getSeatPercent()
        {
            return percent(seats, ResultsReport.this.seats);
        }
    }

    private static BigDecimal tenths(Decimal value)
    {
        return value.toBigDecimal().setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(Decimal numer,
                                      Decimal denom)
    {
        return numer.toBigDecimal()
            .multiply(HUNDRED)
            .divide(denom.toBigDecimal(), 8, RoundingMode.HALF_UP)
            .setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(int amount,
                                      int maxAmount)
    {
        var numer = new Decimal(amount);
        var denom = new Decimal(maxAmount);
        return percent(numer, denom);
    }

    private static String center(String s,
                                 int width)
    {
        return center(s, width, "-");
    }

    private static String center(String s,
                                 int width,
                                 String pad)
    {
        s = " " + s + " ";
        while (s.length() < width) {
            s = pad + s;
            if (s.length() < width) {
                s = s + pad;
            }
        }
        return s;
    }

    private static Party computeWinningParty(Counter<Party> partySeats)
    {
        return partySeats
            .getSortedList()
            .get(0)
            .getKey();
    }
}
