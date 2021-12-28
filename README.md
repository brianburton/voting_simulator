# Voting Sim

This is a fun little project to simulate elections using a proportional representation voting system known as Single
Transferable Vote or [Proportional Ranked Choice Voting](https://www.fairvote.org/prcv#what_is_prcv).

The app sets up multiple elections using multi-member districts of 5 and 3 members. Each district is modeled as having a
randomly selected political affinity (left, right, center, etc). Random ballots are generated for each seat and an STV
election is run to produce winners. Then the party affiliation of the winning candidates are compared to the overall
proportion of first ranked votes for the parties to see how proportional the result really was.

I originally wrote this to play around with different election scenarios to see how proportional STV really is. General
result is that as long as all parties each have some local strength in a number of districts they can hope for a good
proportional result. However a party that has mild strength everywhere but good strength nowhere will not win any seats.

**NOTE:** This is not a scientific simulation!!  The STV election results are computed accurately but the electorates
involved and the votes cast are not representative of the electorate of any real nation. Do not expect results of this
simulation to yield any insights into US voter behavior.
