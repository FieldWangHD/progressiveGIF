package com.kky.wangfang.gif;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * GifDecoder
 */

public class GifManager {
    private GifHeader mHeader;
    private GifFrame mFrame;
    private GifDecoder decoder;
    private int i;
    private ImageView mImageView;
    private InputStream mInputStream;
    private Thread mWorkThread;
    private int mCache;
    private int mDelay = 30;
    private long mLastTime = 0L;
    private String mUrl;
    private boolean isStop;
    private Handler mMainHandler;

    private GifManager(InputStream inputStream, ImageView imageView, String url) {
        mHeader = new GifHeader();
        mFrame = new GifFrame();
        mInputStream = inputStream;
        mImageView = imageView;
        decoder = new GifDecoder();
        mHeader.frames.add(null);
        mHeader.frameCount = 1;
        mUrl = url;
        mFrame.bufferFrameStart = 0;
        mCache = 1024;
        isStop = false;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    private enum State {
        DECODE_ERROR,
        STATE_READY,
        HEADER_OK,
        DECODE_0X21F9,
        DECODE_0X2C_HEADER,
        DECODE_0X2C_COLOR_TAB,
        DECODE_0X2C_BLOCK
    }

    public void start() {
        mWorkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] temp = new byte[mCache];
                try {
                    if (mInputStream == null) {
                        URL url = new URL(mUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                        conn.setConnectTimeout(5*1000);
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Accept", "image/gif");
                        conn.setRequestProperty("Accept-Language", "zh-CN");
                        conn.setRequestProperty("Referer", mUrl);
                        conn.setRequestProperty("Charset", "UTF-8");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:50.0) Gecko/20100101 Firefox/50.0");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.connect();

                        mInputStream = new BufferedInputStream(conn.getInputStream());
                    }
                    int length;
                    while ((length = mInputStream.read(temp)) != -1 && !isStop) {
                        byte[] decode = temp;
                        if (length != mCache) {
                            decode = new byte[length];
                            System.arraycopy(temp, 0, decode, 0, length);
                        }
                        long now = System.currentTimeMillis();
                        decode(decode);
                        //Thread.sleep(10);
                        mLastTime = now;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    isStop = true;
                }
            }
        });
        mWorkThread.start();
    }

    private State prograssState = State.STATE_READY;

    public static class Builder {
        private GifManager decoder;
        private InputStream inputStream;
        private ImageView imageView;
        private String url;

        Builder() {

        }

        public Builder load(InputStream in) {
            inputStream = in;
            return this;
        }

        public Builder load(String url) {
            this.url = url;
            return this;
        }

        public Builder into(ImageView view) {
            imageView = view;
            return this;
        }

        public GifManager build() {
            decoder = new GifManager(inputStream, imageView, url);
            return decoder;
        }
    }

    private byte[] mLastDatas;

    private void decode(byte[] temp) {
        if (mLastDatas != null) {
            byte[] bytes = new byte[temp.length + mLastDatas.length];
            System.arraycopy(mLastDatas, 0, bytes, 0, mLastDatas.length);
            System.arraycopy(temp, 0, bytes, mLastDatas.length, temp.length);

            temp = bytes;
            mLastDatas = null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(temp);
        decode(byteBuffer);
    }

    private void decode(ByteBuffer byteBuffer) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        while (byteBuffer.hasRemaining()) {
            switch (prograssState) {
                case STATE_READY:
                    if(checkRemainAndSave(byteBuffer, 11, 0)) {
                        return;
                    }

                    String id = "";
                    for (int i = 0; i < 6; i++) {
                        id += (char) readByte(byteBuffer);
                    }
                    if (!id.startsWith("GIF")) {
                        prograssState = State.DECODE_ERROR;
                        return;
                    }
                    mHeader.width = byteBuffer.getShort();
                    mHeader.height = byteBuffer.getShort();
                    int packed = readByte(byteBuffer);
                    mHeader.gctFlag = (packed & 0x80) != 0;
                    mHeader.gctSize = 2 << (packed & 7);
                    mHeader.bgColor = readByte(byteBuffer);
                    mHeader.pixelAspect = readByte(byteBuffer);
                    if (mHeader.gctFlag) {
                        if(checkRemainAndSave(byteBuffer, mHeader.gctSize * 3,
                                -11)) {
                            return;
                        }
                        mHeader.gct = readColorTable(mHeader.gctSize, byteBuffer);
                        mHeader.bgColor = mHeader.gct[mHeader.bgColor];
                    }
                    prograssState = State.HEADER_OK;
                    break;
                case HEADER_OK:
                    int code = readByte(byteBuffer);
                    if (code == 0x21) {
                        if (checkRemainAndSave(byteBuffer, 1, -1)) {
                            return;
                        }
                        int sCode = readByte(byteBuffer);
                        if (sCode == 0xF9) {
                            prograssState = State.DECODE_0X21F9;
                        } else if (sCode == 0xFE) {
                            if (!skip(byteBuffer, sCode))
                                return;
                        } else if (sCode == 0x01) {
                            if (!skip(byteBuffer, sCode))
                                return;
                        } else if (sCode == 0xFF) {
                            if (!skip(byteBuffer, sCode))
                                return;
                        } else {
                            if (!skip(byteBuffer, sCode))
                                return;
                        }
                    } else if (code == 0x2C) {
                        prograssState = State.DECODE_0X2C_HEADER;
                    }
                    break;
                case DECODE_0X21F9:
                    if (checkRemainAndSave(byteBuffer, 6, 0)) {
                        return;
                    }
                    readByte(byteBuffer);
                    int F9Packed = readByte(byteBuffer);
                    mFrame.dispose = (F9Packed & 0x1c) >> 2;
                    mFrame.transparency = (F9Packed & 0x01) != 0;
                    mFrame.delay = byteBuffer.getShort();
                    mDelay = mFrame.delay == 0 ? mDelay : mFrame.delay * 10;
                    mFrame.transIndex = readByte(byteBuffer);
                    readByte(byteBuffer);
                    prograssState = State.HEADER_OK;
                    break;
                case DECODE_0X2C_HEADER:
                    if (checkRemainAndSave(byteBuffer, 9, 0)) {
                        return;
                    }
                    mFrame.ix = byteBuffer.getShort();
                    mFrame.iy = byteBuffer.getShort();
                    mFrame.iw = byteBuffer.getShort();
                    mFrame.ih = byteBuffer.getShort();
                    int framePacked = readByte(byteBuffer);
                    mFrame.lctFlag = (framePacked & 0x80) != 0;

                    mFrame.interlace = (framePacked & 0x40) != 0;
                    //mFrame.isSortFlag = (framePacked & 0x20) != 0;
                    mFrame.lctSize = (int) Math.pow(2, (framePacked & 0x07) + 1);

                    if (mFrame.lctFlag) {
                        prograssState = State.DECODE_0X2C_COLOR_TAB;
                    } else {
                        prograssState = State.DECODE_0X2C_BLOCK;
                    }
                    break;
                case DECODE_0X2C_COLOR_TAB:
                    if (checkRemainAndSave(byteBuffer, mFrame.lctSize * 3, 0))
                        return;
                    mFrame.lct = readColorTable(mFrame.lctSize, byteBuffer);
                    prograssState = State.DECODE_0X2C_BLOCK;
                    break;
                case DECODE_0X2C_BLOCK:
                    if (!readBlock(byteBuffer)) {
                        return;
                    }
                    break;
                case DECODE_ERROR:
                    break;
            }
        }
    }

    private int mDataSize;
    private byte[] mImages;
    private boolean isReadLZWLength = false;
    private int mDelayTime = 0;
    private Bitmap mPreBitmap;

    private boolean readBlock(ByteBuffer byteBuffer) {
        if (checkRemainAndSave(byteBuffer, 2, -mDataSize)) {
            mDataSize = 0;
            isReadLZWLength = false;
            return false;
        }
        if (!isReadLZWLength) {
            isReadLZWLength = true;
            readByte(byteBuffer);
            mDataSize++;
        }
        int blockSize = readByte(byteBuffer);
        mDataSize++;

        if (checkRemainAndSave(byteBuffer, blockSize, -mDataSize)) {
            mDataSize = 0;
            isReadLZWLength = false;
            return false;
        }
        mDataSize += blockSize;
        byteBuffer.position(byteBuffer.position() + blockSize);
        if (blockSize <= 0) {
            isReadLZWLength = false;
            mImages = new byte[mDataSize];
            try {
                byteBuffer.position(byteBuffer.position() - mDataSize);
            } catch (Exception e) {
                mDataSize = 0;
                prograssState = State.HEADER_OK;
                return true;
            }
            byteBuffer.get(mImages);
            mHeader.frames.set(0, mFrame);
            if (i++ == 0) {
                decoder.setData(mHeader, mImages);
            } else {
                decoder.setData(mImages, false);
            }
            decoder.setFrameIndex(0);
            final Bitmap bitmap = decoder.getNextFrame();

            long nowTime = System.currentTimeMillis();
            mDelayTime += nowTime - mLastTime > mDelay ? 0 : mDelay - (nowTime - mLastTime);
            if (mImageView != null && !isStop) {
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(bitmap);
                        if (mPreBitmap != null && !bitmap.isRecycled()) {
                            mPreBitmap.recycle();
                        }
                        mPreBitmap = bitmap;
                    }
                }, mDelayTime);
            }
            mLastTime = nowTime;
            mDataSize = 0;
            prograssState = State.HEADER_OK;
        }
        return true;
    }

    private boolean skip(ByteBuffer byteBuffer, int code) {
        try {
            int blockSize;
            do {
                blockSize = readByte(byteBuffer);
                int remain = byteBuffer.limit() - byteBuffer.position();

                if (remain < blockSize) {
                    byteBuffer.position(byteBuffer.position() - 3);
                    mLastDatas = new byte[remain + 3];
                    byteBuffer.get(mLastDatas);
                    mLastDatas[0] = 0x21;
                    mLastDatas[1] = (byte) (code);
                }

                byteBuffer.position(byteBuffer.position() + blockSize);
            } while (blockSize > 0);
        } catch (IllegalArgumentException ex) {
        }
        return true;
    }

    private boolean checkRemainAndSave(ByteBuffer byteBuffer, int number, int offset) {
        int remain = byteBuffer.limit() - byteBuffer.position();

        if (remain < number) {
            byteBuffer.position(byteBuffer.position() + offset);
            mLastDatas = new byte[remain - offset];
            byteBuffer.get(mLastDatas);
        }
        return remain < number;
    }

    private static final int MAX_BLOCK_SIZE = 256;

    private int[] readColorTable(int ncolors, ByteBuffer byteBuffer) {
        int nbytes = 3 * ncolors;
        int[] tab = new int[MAX_BLOCK_SIZE];
        byte[] c = new byte[nbytes];

        byteBuffer.get(c);

        int i = 0;
        int j = 0;
        while (i < ncolors) {
            int r = ((int) c[j++]) & 0xff;
            int g = ((int) c[j++]) & 0xff;
            int b = ((int) c[j++]) & 0xff;
            tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
        return tab;
    }

    public void clear() {
        if (mPreBitmap != null && !mPreBitmap.isRecycled()) {
            mPreBitmap.recycle();
            mPreBitmap = null;
        }
        isStop = true;

        mMainHandler.removeCallbacksAndMessages(null);
    }

    private int readByte(ByteBuffer byteBuffer) {
        return 0xFF & byteBuffer.get();
    }

}
