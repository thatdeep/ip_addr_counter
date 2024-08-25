import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class UniqueIPCounterBitArrayParallel {
    public static void main(String[] args) throws Exception {
        var clockStart = System.currentTimeMillis();
        calculate();
        System.err.format("Took %,d ms\n", System.currentTimeMillis() - clockStart);
    }

    private static void calculate() throws Exception {
        final File file = new File("ip_addresses.txt");
        final long length = file.length();
        final int NUM_BITMASKS = 256;
        final int BITMASK_SIZE = 524288;
        final int chunkCount = Runtime.getRuntime().availableProcessors();
        final AtomicIntegerArray[] bitMasks = new AtomicIntegerArray[NUM_BITMASKS];
        for (int i = 0; i < NUM_BITMASKS; i++) {
            bitMasks[i] = new AtomicIntegerArray(BITMASK_SIZE);
        }
        final var chunkStartOffsets = new long[chunkCount];
        try (var raf = new RandomAccessFile(file, "r")) {
            for (int i = 1; i < chunkStartOffsets.length; i++) {
                var start = length * i / chunkStartOffsets.length;
                raf.seek(start);
                while (raf.read() != (byte) '\n') {
                }
                start = raf.getFilePointer();
                chunkStartOffsets[i] = start;
            }
            final var mappedFile = raf.getChannel().map(MapMode.READ_ONLY, 0, length, Arena.global());
            var threads = new Thread[chunkCount];
            for (int i = 0; i < chunkCount; i++) {
                final long chunkStart = chunkStartOffsets[i];
                final long chunkLimit = (i + 1 < chunkCount) ? chunkStartOffsets[i + 1] : length;
                threads[i] = new Thread(new ChunkProcessor(
                        mappedFile.asSlice(chunkStart, chunkLimit - chunkStart), bitMasks, i));
            }
            for (var thread : threads) {
                thread.start();
            }
            for (var thread : threads) {
                thread.join();
            }
        }
        long uniqueCount = 0;
        for (int i = 0; i < NUM_BITMASKS; i++) {
            for (int j = 0; j < BITMASK_SIZE; j++) {
                uniqueCount += Integer.bitCount(bitMasks[i].get(j) & 0xFFFFFFFF);
            }
        }
        System.out.println("Number of unique IP addresses: " + uniqueCount);
    }

    private static class ChunkProcessor implements Runnable {
        private final MemorySegment chunk;
        private final AtomicIntegerArray[] bitMasks;
        private final int myIndex;

        ChunkProcessor(MemorySegment chunk, AtomicIntegerArray[] bitMasks, int myIndex) {
            this.chunk = chunk;
            this.bitMasks = bitMasks;
            this.myIndex = myIndex;
        }

        @Override public void run() {
            for (var cursor = 0L; cursor < chunk.byteSize();) {
                var dotPos1 = findByte(cursor, '.');
                int ip1 = parseSubAddress(cursor, dotPos1);
                var dotPos2 = findByte(dotPos1 + 1, '.');
                int ip2 = parseSubAddress(dotPos1 + 1, dotPos2);
                var dotPos3 = findByte(dotPos2 + 1, '.');
                int ip3 = parseSubAddress(dotPos2 + 1, dotPos3);
                var dotPos4 = findByte(dotPos3 + 1, '\n');
                int ip4 = parseSubAddress(dotPos3 + 1, dotPos4);

                long globalIndex = ((long)ip1 << (24)) + ((long)ip2 << (16)) + ((long)ip3 << (8)) + (long)ip4;
                int bitMaskIndex = (int)(globalIndex / 16777216);
                int arrayIndex = (int)(globalIndex % 16777216);
                int bitOffset = (int)(arrayIndex / 32);
                int bitIndex = (int)(globalIndex % 32);

                while (true) {
                    int value = bitMasks[bitMaskIndex].get(bitOffset);
                    int newValue = value | (1 << bitIndex);
                    if (bitMasks[bitMaskIndex].compareAndSet(bitOffset, value, newValue)) {
                        break;
                    }
                }
                cursor = dotPos4 + 1;
            }
        }

        private int parseSubAddress(long start, long end) {
            int value = 0;
            for (var i = start; i < end; i++) {
                byte b = chunk.get(JAVA_BYTE, i);
                value = value * 10 + (b - '0');
            }
            return value;
        }

        private long findByte(long cursor, int b) {
            for (var i = cursor; i < chunk.byteSize(); i++) {
                if (chunk.get(JAVA_BYTE, i) == b) {
                    return i;
                }
            }
            throw new RuntimeException(((char) b) + " not found");
        }
    }
}