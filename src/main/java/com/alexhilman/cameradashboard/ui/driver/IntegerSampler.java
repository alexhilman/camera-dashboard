package com.alexhilman.cameradashboard.ui.driver;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Samples a given number of integer values and provides operations for them.
 */
@NotThreadSafe
public class IntegerSampler {
    private final int[] motionPixelsByFrame;
    private int currentPointer;

    IntegerSampler(final int slots) {
        this.motionPixelsByFrame = new int[slots];
    }

    /**
     * Sets the oldest slot in the sampler to the specified number.
     *
     * @param num
     */
    public void sample(final int num) {
        motionPixelsByFrame[currentPointer++] = num;
        if (currentPointer >= motionPixelsByFrame.length) {
            currentPointer = 0;
        }
    }

    /**
     * Creates a new sampler with the given slots to record values.
     *
     * @param slots Number of slots for the sampler
     * @return Sampler
     */
    public static IntegerSampler forSamples(final int slots) {
        return new IntegerSampler(slots);
    }

    /**
     * Calculate the current average of integers in the slots.
     *
     * @return Average value
     */
    public int average() {
        long sum = 0;
        for (int i = 0; i < motionPixelsByFrame.length; i++) {
            sum += motionPixelsByFrame[i];
        }
        return Math.toIntExact(sum / motionPixelsByFrame.length);
    }
}