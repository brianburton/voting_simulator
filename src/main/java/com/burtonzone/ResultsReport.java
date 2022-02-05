package com.burtonzone;

import com.burtonzone.common.Counter;
import com.burtonzone.common.Decimal;
import com.burtonzone.election.ElectionResult;
import com.burtonzone.election.Party;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Builder;
import lombok.Value;
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
    int votes;
    int effectiveVotes;
    @Builder.Default
    Counter<Party> partyVotes = new Counter<>();
    @Builder.Default
    Counter<Party> partySeats = new Counter<>();

    public static ResultsReport of(ElectionResult result)
    {
        final var seats = result.getElection().getSeats();
        final var elected = result.getFinalRound().getElected().size();
        final var votes = result.getElection().getTotalVotes().toInt();
        final var effectiveVotes = result.getEffectiveFirstVoteCount();
        final var partyVotes = result.getPartyFirstChoiceCounts();
        final var partyElected = result.getPartyElectedCounts();
        return builder()
            .parties(JImmutables.insertOrderSet(result.getElection().getParties()))
            .seats(seats)
            .elected(elected)
            .votes(votes)
            .effectiveVotes(effectiveVotes)
            .partyVotes(partyVotes)
            .partySeats(partyElected)
            .build();
    }

    public static ResultsReport of(Iterable<ElectionResult> results)
    {
        var answer = builder().build();
        for (ElectionResult result : results) {
            answer = answer.add(of(result));
        }
        return answer;
    }

    public ResultsReport add(ResultsReport other)
    {
        return builder()
            .parties(parties.insertAll(other.parties))
            .seats(seats + other.seats)
            .elected(elected + other.elected)
            .votes(votes + other.votes)
            .effectiveVotes(effectiveVotes + other.effectiveVotes)
            .partyVotes(partyVotes.add(other.partyVotes))
            .partySeats(partySeats.add(other.partySeats))
            .build();
    }

    // https://en.wikipedia.org/wiki/Gallagher_index
    public Decimal computeErrors()
    {
        final var totalSeats = partySeats.getTotal();
        final var totalVotes = partyVotes.getTotal();
        var sum = Decimal.ZERO;
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
            out.printf("%-3s %-3s   ", "", "");
            for (Party party : parties) {
                out.printf("  %s ", center(party.getName(), 20));
            }
        }
        return str.toString();
    }

    public static String printHeader2(Iterable<Party> parties)
    {
        StringWriter str = new StringWriter();
        try (PrintWriter out = new PrintWriter(str)) {
            out.printf("%3s %3s   ", "rsc", "rec");
            for (Party party : parties) {
                out.printf(" %4s   %6s  %6s ", "ps", "eps", "aps");
            }
            out.printf(" %6s %6s", "err", "eff");
        }
        return str.toString();
    }

    public String getRow()
    {
        StringWriter str = new StringWriter();
        try (PrintWriter out = new PrintWriter(str)) {
            out.printf(" %3d %3d  ", seats, elected);
            for (Party party : parties) {
                final var pr = new PartyResult(party);
                out.printf("  %4d   %5s%%  %5s%%", pr.getSeats(), pr.getVotePercent(), pr.getSeatPercent());
            }
            out.printf("  %5s%% %5s%%", percent(computeErrors(), Decimal.ONE), percent(effectiveVotes, votes));
        }
        return str.toString();
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
            return percent(getVotes(), votes);
        }

        public BigDecimal getSeatPercent()
        {
            return percent(getSeats(), seats);
        }
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
        s = " " + s + " ";
        while (s.length() < width) {
            s = "-" + s;
            if (s.length() < width) {
                s = s + "-";
            }
        }
        return s;
    }
}
