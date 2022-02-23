# Voting Sim

This is a fun little project to simulate elections using a variety of proportional representation voting systems. The
app sets up multiple elections using multi-member districts various configurations including the
[FairVote Proposed Districts](https://www.fairvote.org/fair_rep_in_congress#the_fair_representation_act_in_your_state_ceuyfcksbeh7qolrydoazw)
of 5 and 3 members.

Each simulation run follows these steps in each district:

- A set of parties are randomly positioned in a two-dimensional issue space.
- Candidates for each party are randomly positioned near their party position in the issue space.
- A "voter center" is chosen by taking an average position within a subset of the parties.
- Voters are randomly positioned in the issue space with a preference to be near the voter center.
- Ranked candidate lists are generated for each voter using a configurable strategy (see below) to create a ballot box.

The ranking of candidates by each voter is controlled by a voting type configuration setting. Possible types include:

- Candidate centered: Each voter selects a number of candidates nearest to the voter in the issue space without
  consideration of the candidate's party. This is what you normally hear about with STV.
- Party centered: Each voter selects the party candidate list from the parties closest to the voter in the issue space.
  This is similar to modern Australian Senate "above the line" voting.
- Single party: Each voter selects all candidates from a single party and ranks them by their distance to the voter in
  the issue space. This is the usual voter behavior for partisan voters.
- Mixed: Voters randomly choose to vote using either party centered or candidate centered on a configurable probability.
  This models the "above the line" vs "below the line" Australian Senate election ballot options.

Once the ballots have been assigned the election in each district is resolved using one the election runners. Each
runner corresponds to a particular election system. Note that while the ballots always have ranked candidates the
election runners other than STV simply pick one or more candidates and use them in non-ranked order. Available election
runners are:

- Party list using [D'Hondt method](https://en.wikipedia.org/wiki/D%27Hondt_method).
- Party list using [Webster/Sainte-Laguë method](https://en.wikipedia.org/wiki/Webster/Sainte-Lagu%C3%AB_method).
- Party list using [Largest_remainder_method](https://en.wikipedia.org/wiki/Largest_remainder_method).
- [Proportional Single Transferable Vote](https://www.fairvote.org/prcv#what_is_prcv).
- [Plurality block voting](https://en.wikipedia.org/wiki/Plurality_block_voting) (non-proportional).
- [Limited voting](https://en.wikipedia.org/wiki/Limited_voting) (semi-proportional).
- Plurality single vote (non-proportional).

Once all of the elections have been executed the results are combined into a total outcome. Various metrics are computed
at the distict and aggregate level to gauge the quality of the results:

| Heading | Description |
| ------- | ----------- |
| rsc     | Number of seats up for election. |
| rec     | Number of seats filled in election (should always equal `rsc`) |
| eps     | Expected seats for party based on vote percentage. Top line is percentage of total seats. Bottom line is number of seats. |
| aps     | Actual seats won by party during election. Top line is percentage of total seats. Bottom line is number of seats. Ideally same as `eps`. |
| ranks   | Average number of candidates ranked by each voter. |
| waste   | Percentage of votes whose votes failed to elect any candidate. Lower numbers are better. Zero would be ideal but is not possible. |
| err     | Total percentage difference between `eps` and `aps` for all parties. Range is between 0% and 50%. Lower numbers are better.  |
| eff     | Voter effectiveness. Score indicating how satisfied voters would be with results based on their candidate rankings vs which candidates won seats. |

For `waste`, `err`, and `eff` the top line contains the overall value computed across the entire "nation". The bottom
line contains an average of the values in each district. The nationwide (top line) values tend to smooth over the
inaccuracies and make the system look much better than it really is. The bottom line numbers should be given more weight
when comparing systems since those more accurately illustrate unfairness at the district level. For example national
election results using gerrymandered single seat districts can look fairly reasonable in aggregate but when you look at
the district level you'll see huge disparities and many districts electing candidates with less than a majority of the
votes. The block vote is particularly unfair and can award all seats to plurality parties at the district level.

After the individual election results the app prints a table showing possible party coalitions that would produce
majorities in the congress. The fields in the table are:

| Heading | Description                                                                                                                      |
|---------|----------------------------------------------------------------------------------------------------------------------------------|
| W       | An asterisk here indicates that the coalition includes the party that won the most seats.                                        |
| Dist    | The maximum distance in the issue grid between any two parties in the coalition. Closer parties produce more stable coalitions. | 
| Vote%   | The total percentage of the vote won by the parties in the coalition. |
| Seat%   | The total percentage of the vote won by the partiesin the coalition. |
| Parties | Parties in the coalition. |

Each district is modeled as having a randomly selected political affinity (left, right, center, etc). Random ballots are
generated for each seat and an STV election is run to produce winners. Then the party affiliation of the winning
candidates are compared to the overall proportion of first ranked votes for the parties to see how proportional the
result really was.

**NOTE:** This is not a scientific simulation!!  The STV election results are computed accurately but the electorates
involved and the votes cast are not representative of the electorate of any real nation. Do not expect results of this
simulation to yield any insights into US voter behavior.
