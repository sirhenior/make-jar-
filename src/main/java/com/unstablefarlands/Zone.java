package com.unstablefarlands;

public interface Zone {
    String id();

    /** Fill one world column (wx, wz). Must be deterministic: same input, same output. */
    void column(int wx, int wz, Col c, Noise n);

    /** Optional: place trees/decoration. Called once per chunk. */
    default void decorate(Deco d) {}

    /** If true, players inside get the Darkness effect. */
    default boolean dark() { return false; }
}
