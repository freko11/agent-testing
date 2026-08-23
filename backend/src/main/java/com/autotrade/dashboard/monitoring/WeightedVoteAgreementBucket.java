package com.autotrade.dashboard.monitoring;

/**
 * How {@link com.autotrade.dashboard.signal.WeightedVoteRuleEngine}'s replayed call compares to
 * what was actually recorded on a {@code SignalCallEntry} (E8-F5-S3). Exactly these four buckets
 * are exhaustive, not an arbitrary subset — see {@link WeightedVoteShadowScoringService}'s class
 * Javadoc for the invariant that makes a fifth ("flipped direction") bucket structurally
 * impossible given how the two engines share {@code SignalRuleEngine#computeVotes}.
 */
public enum WeightedVoteAgreementBucket {

    /** Both engines resolve the same {@code SignalCall} (including HOLD == HOLD). */
    AGREE,

    /** The recorded call was not BUY (almost always HOLD), but the weighted engine resolves BUY —
     * a call the weighted engine would have added. */
    WEIGHTED_ONLY_BUY,

    /** The recorded call was not SELL (almost always HOLD), but the weighted engine resolves
     * SELL — a call the weighted engine would have added. */
    WEIGHTED_ONLY_SELL,

    /** The recorded call was BUY or SELL, but the weighted engine resolves HOLD — a call the
     * weighted engine would have suppressed. */
    DOWNGRADED_BY_WEIGHTED
}
