/**
 * Copyright 2014 Google, Inc. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.kky.wangfang.gif;

import java.util.Arrays;

/**
 * Inner model class housing metadata for each frame.
 */
class GifFrame {
    int ix, iy, iw, ih;
    /**
     * Control Flag.
     */
    boolean interlace;
    boolean isControlInit;
    boolean isIn21;
    /**
     * Control Flag.
     */
    boolean transparency;
    /**
     * Disposal Method.
     */
    int dispose;
    /**
     * Transparency Index.
     */
    int transIndex;
    /**
     * Delay, in ms, to next frame.
     */
    int delay;
    /**
     * Index in the raw buffer where we need to start reading to decode.
     */
    int bufferFrameStart;
    /**
     * Local Color Table.
     */
    int[] lct;
    public boolean lctFlag;
    public int lctSize;

    public boolean isInitStart;
    public boolean initImage;

    public GifFrame() {
        ix = -1;
        iy = -1;
        iw = -1;
        ih = -1;
        isControlInit = false;
        isInitStart = false;
        initImage = false;
        isIn21 = false;
    }

    @Override
    public String toString() {
        return "GifFrame{" +
                "ix=" + ix +
                ", iy=" + iy +
                ", iw=" + iw +
                ", ih=" + ih +
                ", interlace=" + interlace +
                ", isControlInit=" + isControlInit +
                ", isIn21=" + isIn21 +
                ", transparency=" + transparency +
                ", dispose=" + dispose +
                ", transIndex=" + transIndex +
                ", delay=" + delay +
                ", bufferFrameStart=" + bufferFrameStart +
                ", lct=" + Arrays.toString(lct) +
                ", lctFlag=" + lctFlag +
                ", lctSize=" + lctSize +
                ", isInitStart=" + isInitStart +
                ", initImage=" + initImage +
                '}';
    }
}