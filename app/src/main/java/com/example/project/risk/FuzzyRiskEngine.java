package com.example.project.risk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SafeWalk fuzzy risk engine (Mamdani model).
 *
 * Pipeline: fuzzification -> rule evaluation (min AND) -> aggregation (max) -> centroid defuzzification
 *
 * Inputs:
 *   stationaryMinutes : how long the user has been stationary, in minutes
 *   deviationMeters    : distance from the planned route, in metres
 *   hoursSince6pm      : time of night, expressed as hours since 18:00 (0..11)
 *
 * Output:
 *   risk score in [0, 100]
 */
public class FuzzyRiskEngine {

    // ---------- membership functions ----------

    public static double triangular(double x, double a, double b, double c) {
        // x == b is checked FIRST, before the outer-bounds bail-out. When a == b (a left-edge
        // "spike" set, e.g. Short-stationary or Small-deviation, which peak exactly at 0), the
        // old ordering had x <= a trigger on x == a == b and return 0.0 before ever reaching the
        // x == b check - so a reading of exactly 0 was read as NOT belonging to its own peak set
        // at all. That's what let "stationary 20+ min, deviation 0m" score as LOW with no rule
        // firing: Small/Early/Short all degenerately zeroed out right at the walk's start values.
        if (x == b) return 1.0;
        if (x <= a || x >= c) return 0.0;
        if (x <= b) return ((x - a) / (b - a));
        return ((c - x) / (c - b));
    }

    public static double trapezoid(double x, double a, double b, double c, double d) {
        if (x <= a || x >= d) return 0.0;
        if (x >= a && x <= b) return ((x - a) / (b - a));
        if (x >= b && x <= c) return 1.0;
        if (x >= c && x <= d) return ((d - x) / (d - c));
        return 0.0;
    }

    /**
     * Right-shoulder membership: 0 below a, ramps to 1 at b, then STAYS at 1 forever after —
     * unlike trapezoid(), it never drops back to 0 for large x. This is the correct shape for
     * open-ended categories like "Long" stationary time or "Large" deviation, where any value
     * past the threshold should count as fully belonging to the set, not just values up to some
     * arbitrary cap.
     */
    public static double rightShoulder(double x, double a, double b) {
        if (x <= a) return 0.0;
        if (x >= b) return 1.0;
        return (x - a) / (b - a);
    }

    // ---------- fuzzification: compute membership degrees for each input ----------
    //
    // `scale` shifts the fuzzy set BOUNDARIES, not the raw sensor reading. Scaling the raw
    // reading instead can push a real value outside a bounded set's domain and silently zero
    // out the whole computation. Shifting the boundaries keeps every real-world reading
    // meaningful while still making the sets easier (scale < 1) or harder (scale > 1) to
    // fully satisfy.

    private static Map<String, Double> fuzzifyStationary(double minutes, double scale) {
        Map<String, Double> degrees = new HashMap<>();
        degrees.put("Short", triangular(minutes, 0, 0, 3 * scale));
        degrees.put("Moderate", triangular(minutes, 1 * scale, 4 * scale, 7 * scale));
        degrees.put("Long", rightShoulder(minutes, 5 * scale, 8 * scale));
        return degrees;
    }

    private static Map<String, Double> fuzzifyDeviation(double meters, double scale) {
        Map<String, Double> degrees = new HashMap<>();
        degrees.put("Small", triangular(meters, 0, 0, 60 * scale));
        degrees.put("Medium", triangular(meters, 30 * scale, 90 * scale, 150 * scale));
        degrees.put("Large", rightShoulder(meters, 120 * scale, 160 * scale));
        return degrees;
    }

    private static Map<String, Double> fuzzifyNight(double hoursSince6pm, double scale) {
        Map<String, Double> degrees = new HashMap<>();
        degrees.put("Early", triangular(hoursSince6pm, 0, 0, 3 * scale));
        degrees.put("Late", triangular(hoursSince6pm, 2 * scale, 5 * scale, 8 * scale));
        degrees.put("VeryLate", rightShoulder(hoursSince6pm, 6 * scale, 9 * scale));
        return degrees;
    }

    // ---------- output fuzzy sets (risk score 0..100) ----------

    private static double outputMembership(String set, double x) {
        switch (set) {
            case "Low": return triangular(x, 0, 0, 40);
            case "Medium": return triangular(x, 25, 50, 75);
            case "High": return triangular(x, 60, 100, 100);
            default: return 0.0;
        }
    }

    // ---------- rule base ----------
    // Each rule: two (variable, set) antecedents ANDed with min(), mapped to one output set.

    private static class Rule {
        String var1, set1, var2, set2, output;
        Rule(String var1, String set1, String var2, String set2, String output) {
            this.var1 = var1; this.set1 = set1;
            this.var2 = var2; this.set2 = set2;
            this.output = output;
        }
    }

    private static final Rule[] RULES = {
            new Rule("st", "Long", "night", "VeryLate", "High"),
            new Rule("dev", "Large", "night", "Late", "High"),
            new Rule("dev", "Large", "st", "Long", "High"),
            new Rule("night", "VeryLate", "dev", "Medium", "High"),
            new Rule("dev", "Small", "st", "Short", "Low"),
            new Rule("night", "Early", "dev", "Small", "Low"),
            new Rule("st", "Moderate", "dev", "Medium", "Medium"),
            new Rule("st", "Short", "night", "VeryLate", "Medium"),
            new Rule("dev", "Medium", "night", "Late", "Medium"),
            new Rule("st", "Long", "dev", "Small", "Medium"),
    };

    // Human-readable labels used only for the live "why this score" explanation panel - purely
    // presentational, they don't feed back into the math at all.
    private static String friendlyVar(String var) {
        switch (var) {
            case "st": return "Stationary time";
            case "dev": return "Route deviation";
            case "night": return "Time of night";
            default: return var;
        }
    }

    private static String friendlySet(String var, String set) {
        switch (var + "." + set) {
            case "st.Short": return "briefly still";
            case "st.Moderate": return "stationary a while";
            case "st.Long": return "stationary a long time";
            case "dev.Small": return "on route";
            case "dev.Medium": return "off route";
            case "dev.Large": return "far off route";
            case "night.Early": return "daytime/early evening";
            case "night.Late": return "late at night";
            case "night.VeryLate": return "very late at night";
            default: return set;
        }
    }

    private static class FiredRule {
        double strength;
        String output;
        Rule rule;
        FiredRule(double strength, String output, Rule rule) {
            this.strength = strength;
            this.output = output;
            this.rule = rule;
        }
    }

    /**
     * Sensitivity presets shift how quickly the engine escalates, by scaling the fuzzy set
     * BOUNDARIES — never the raw sensor reading.
     * Scale > 1 makes the boundaries bigger (Relaxed: takes more stationary time / deviation /
     *   lateness to reach the same membership degree, so it escalates later).
     * Scale < 1 makes the boundaries smaller (Cautious: reaches the same membership degree with
     *   less stationary time / deviation / lateness, so it escalates sooner).
     */
    public enum Sensitivity {
        RELAXED(1.25),
        STANDARD(1.0),
        CAUTIOUS(0.75);

        public final double scale;
        Sensitivity(double scale) { this.scale = scale; }
    }

    /** Result of a full inference pass: the numeric score plus a plain-English explanation of
     *  the single strongest-firing rule, for the live "why this score?" panel. */
    public static class RiskExplanation {
        public final double risk;
        public final String summary;
        public RiskExplanation(double risk, String summary) {
            this.risk = risk;
            this.summary = summary;
        }
    }

    // ---------- public API ----------

    public static double computeRisk(double stationaryMinutes, double deviationMeters, double hoursSince6pm) {
        return computeRisk(stationaryMinutes, deviationMeters, hoursSince6pm, Sensitivity.STANDARD);
    }

    public static double computeRisk(double stationaryMinutes, double deviationMeters, double hoursSince6pm,
                                     Sensitivity sensitivity) {
        return computeRiskExplained(stationaryMinutes, deviationMeters, hoursSince6pm, sensitivity).risk;
    }

    /**
     * Same inference pass as computeRisk(), but also reports which single rule contributed the
     * most to the final aggregated output - this is what lets the UI show the user WHY the
     * score is what it is, instead of the number appearing out of nowhere.
     */
    public static RiskExplanation computeRiskExplained(double stationaryMinutes, double deviationMeters,
                                                         double hoursSince6pm, Sensitivity sensitivity) {
        double scale = sensitivity.scale;
        Map<String, Double> stDeg = fuzzifyStationary(stationaryMinutes, scale);
        Map<String, Double> devDeg = fuzzifyDeviation(deviationMeters, scale);
        Map<String, Double> nightDeg = fuzzifyNight(hoursSince6pm, scale);

        Map<String, Map<String, Double>> lookup = new HashMap<>();
        lookup.put("st", stDeg);
        lookup.put("dev", devDeg);
        lookup.put("night", nightDeg);

        FiredRule[] fired = new FiredRule[RULES.length];
        for (int i = 0; i < RULES.length; i++) {
            Rule r = RULES[i];
            double deg1 = lookup.get(r.var1).get(r.set1);
            double deg2 = lookup.get(r.var2).get(r.set2);
            double strength = Math.min(deg1, deg2);
            fired[i] = new FiredRule(strength, r.output, r);
        }

        double numerator = 0.0;
        double denominator = 0.0;
        for (int x = 0; x <= 100; x++) {
            double mu = 0.0;
            for (FiredRule fr : fired) {
                if (fr.strength <= 0.0) continue;
                double clipped = Math.min(fr.strength, outputMembership(fr.output, x));
                if (clipped > mu) mu = clipped;
            }
            numerator += x * mu;
            denominator += mu;
        }
        double risk = denominator > 0.0 ? numerator / denominator : 0.0;

        FiredRule top = null;
        for (FiredRule fr : fired) {
            if (top == null || fr.strength > top.strength) top = fr;
        }

        String summary;
        if (top == null || top.strength <= 0.0) {
            summary = "No rule is firing yet — conditions are nominal.";
        } else {
            summary = String.format(Locale.getDefault(),
                    "%s (%s, %.0f%%) + %s (%s, %.0f%%) → %s risk rule at %.0f%% strength",
                    friendlyVar(top.rule.var1), friendlySet(top.rule.var1, top.rule.set1),
                    lookup.get(top.rule.var1).get(top.rule.set1) * 100,
                    friendlyVar(top.rule.var2), friendlySet(top.rule.var2, top.rule.set2),
                    lookup.get(top.rule.var2).get(top.rule.set2) * 100,
                    top.output, top.strength * 100);
        }

        return new RiskExplanation(risk, summary);
    }

    public static RiskTier tierFor(double riskScore) {
        if (riskScore < 30.0) return RiskTier.LOW;
        if (riskScore < 65.0) return RiskTier.MEDIUM;
        return RiskTier.HIGH;
    }

    /**
     * Hours elapsed since 6pm, for the current night window only (6pm-6am, 12 hours).
     * The naive ((hour-18+24)%24) wraps DAYTIME hours (6am-6pm) into 12-23 "hours since
     * 6pm", which the fuzzy sets below then read as deep into "VeryLate" - so a 2pm walk
     * was scoring as if it were the middle of the night. Daytime isn't part of the night
     * window at all, so it's clamped to 0 (fully "Early"/not-late) instead of wrapping.
     */
    public static double hoursSince6pmFromClock(int hour24, int minute) {
        int totalMinutesFrom6pm = ((hour24 - 18 + 24) % 24) * 60 + minute;
        double hours = totalMinutesFrom6pm / 60.0;
        if (hours > 12.0) return 0.0;
        return hours;
    }

    public enum RiskTier { LOW, MEDIUM, HIGH }
}
