package com.farmrpg.jobs;

/** Clears only near-black pixels connected to the outer margin; the source artwork is unchanged. */
final class GuildAlchemyEdgeMask {
    private GuildAlchemyEdgeMask() {}

    static void apply(int[] pixels, int width, int height) {
        if (width <= 0 || height <= 0 || (long) width * height != pixels.length) {
            throw new IllegalArgumentException("Invalid image size");
        }
        int[] queue = new int[pixels.length];
        boolean[] seen = new boolean[pixels.length];
        int tail = 0;
        for (int x = 0; x < width; x++) {
            tail = enqueue(x, pixels, width, height, seen, queue, tail);
            tail = enqueue((height - 1) * width + x, pixels, width, height, seen, queue, tail);
        }
        for (int y = 0; y < height; y++) {
            tail = enqueue(y * width, pixels, width, height, seen, queue, tail);
            tail = enqueue(y * width + width - 1, pixels, width, height, seen, queue, tail);
        }
        for (int head = 0; head < tail; head++) {
            int index = queue[head];
            int x = index % width, y = index / width;
            pixels[index] &= 0x00FFFFFF;
            if (x > 0) tail = enqueue(index - 1, pixels, width, height, seen, queue, tail);
            if (x + 1 < width) tail = enqueue(index + 1, pixels, width, height, seen, queue, tail);
            if (y > 0) tail = enqueue(index - width, pixels, width, height, seen, queue, tail);
            if (y + 1 < height) tail = enqueue(index + width, pixels, width, height, seen, queue, tail);
        }
    }

    private static int enqueue(int index, int[] pixels, int width, int height,
            boolean[] seen, int[] queue, int tail) {
        if (seen[index]) return tail;
        seen[index] = true;
        int x = index % width, y = index / width;
        int horizontalMargin = Math.max(1, width / 16);
        int verticalMargin = Math.max(1, height / 16);
        if (x >= horizontalMargin && x < width - horizontalMargin
                && y >= verticalMargin && y < height - verticalMargin) return tail;
        int color = pixels[index]; // NativeImage uses ABGR byte order.
        int alpha = color >>> 24, red = color & 255, green = (color >>> 8) & 255, blue = (color >>> 16) & 255;
        if (alpha == 0 || (red <= 24 && green <= 24 && blue <= 32)) queue[tail++] = index;
        return tail;
    }
}
