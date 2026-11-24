package com.pvpai;

import java.util.Random;

/** Small neural network (11 inputs, 14 hidden, 5 outputs). Trains by backprop and evolves by mutation. */
public class Net {
    public static final int NIN = 11, NH = 14, NOUT = 5;
    public float[] w;
    private transient float[] hid = new float[NH], out = new float[NOUT];

    public Net() {
        w = new float[NH * NIN + NH + NOUT * NH + NOUT];
        Random r = new Random();
        for (int i = 0; i < w.length; i++) w[i] = (float) (r.nextGaussian() * 0.3);
    }

    public Net copy() { Net n = new Net(); n.w = w.clone(); return n; }

    public void mutate(double sigma, Random r) {
        for (int i = 0; i < w.length; i++) if (r.nextDouble() < 0.2) w[i] += (float) (r.nextGaussian() * sigma);
    }

    public float[] forward(float[] x) {
        if (hid == null) { hid = new float[NH]; out = new float[NOUT]; }
        int b1 = NH * NIN, w2 = b1 + NH, b2 = w2 + NOUT * NH;
        for (int j = 0; j < NH; j++) {
            float s = w[b1 + j];
            for (int i = 0; i < NIN; i++) s += x[i] * w[j * NIN + i];
            hid[j] = (float) Math.tanh(s);
        }
        for (int k = 0; k < NOUT; k++) {
            float s = w[b2 + k];
            for (int j = 0; j < NH; j++) s += hid[j] * w[w2 + k * NH + j];
            out[k] = (float) Math.tanh(s);
        }
        return out;
    }

    /** One step of gradient descent toward the target outputs. */
    public void train(float[] x, float[] t, float lr) {
        forward(x);
        int b1 = NH * NIN, w2 = b1 + NH, b2 = w2 + NOUT * NH;
        float[] dO = new float[NOUT], dH = new float[NH];
        for (int k = 0; k < NOUT; k++) dO[k] = (out[k] - t[k]) * (1 - out[k] * out[k]);
        for (int j = 0; j < NH; j++) {
            float s = 0;
            for (int k = 0; k < NOUT; k++) s += dO[k] * w[w2 + k * NH + j];
            dH[j] = s * (1 - hid[j] * hid[j]);
        }
        for (int k = 0; k < NOUT; k++) {
            for (int j = 0; j < NH; j++) w[w2 + k * NH + j] -= lr * dO[k] * hid[j];
            w[b2 + k] -= lr * dO[k];
        }
        for (int j = 0; j < NH; j++) {
            for (int i = 0; i < NIN; i++) w[j * NIN + i] -= lr * dH[j] * x[i];
            w[b1 + j] -= lr * dH[j];
        }
    }
}
