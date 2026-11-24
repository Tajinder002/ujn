package com.pvpai;

import java.util.Random;

/** The AI's "skills". Evolves after every death. */
public class Genome {
    public double aimSpeed = 24;      // max degrees per tick it can turn toward the target
    public double strafePeriod = 14;  // ticks between strafe direction changes
    public double range = 2.8;        // distance it tries to hold (blocks)
    public double critChance = 0.5;   // chance a swing is a jump-crit
    public double hitDelay = 0.92;    // attack cooldown fraction before swinging

    public Genome copy() {
        Genome g = new Genome();
        g.aimSpeed = aimSpeed; g.strafePeriod = strafePeriod; g.range = range;
        g.critChance = critChance; g.hitDelay = hitDelay;
        return g;
    }

    static double c(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    public void clamp() {
        aimSpeed = c(aimSpeed, 6, 60);
        strafePeriod = c(strafePeriod, 5, 30);
        range = c(range, 2.2, 3.0);
        critChance = c(critChance, 0, 1);
        hitDelay = c(hitDelay, 0.6, 1.0);
    }

    public void mutate(double amt, Random r) {
        aimSpeed *= 1 + r.nextGaussian() * amt;
        strafePeriod *= 1 + r.nextGaussian() * amt;
        range += r.nextGaussian() * amt;
        critChance += r.nextGaussian() * amt;
        hitDelay += r.nextGaussian() * amt * 0.3;
        clamp();
    }
}
