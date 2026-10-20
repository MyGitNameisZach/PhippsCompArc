public class L2Cache {
    private static final int LINES = 4;
    private static final int LINE_SIZE = 8;

    private boolean[] valid = new boolean[LINES];
    private int[] tags = new int[LINES];
    private Word32[][] data = new Word32[LINES][LINE_SIZE];

    public Memory mainMemory;

    // Simple FIFO replacement counter
    private int replacePointer = 0;

    public L2Cache(Memory mem) {
        this.mainMemory = mem;
        for (int i = 0; i < LINES; i++) {
            for (int j = 0; j < LINE_SIZE; j++) {
                data[i][j] = new Word32();
            }
        }
    }

    /**
     * Read a full block (for the instruction cache).
     * Returns the block and stores the cycle count in outCycles[0].
     */
    public Word32[] readBlock(int blockAddr, int[] outCycles) {
        // Check if block is already in cache
        for (int i = 0; i < LINES; i++) {
            if (valid[i] && tags[i] == blockAddr) {
                outCycles[0] = 20;              // L2 hit
                return data[i];
            }
        }

        // Miss – fetch from main memory (340 cycles)
        Word32[] block = new Word32[LINE_SIZE];
        for (int i = 0; i < LINE_SIZE; i++) {
            int wordAddr = blockAddr + i;
            block[i] = new Word32();
            readMemoryWord(wordAddr, block[i]);
        }

        // Fill the chosen line
        int line = replacePointer;
        replacePointer = (replacePointer + 1) % LINES;
        for (int i = 0; i < LINE_SIZE; i++) {
            block[i].copy(data[line][i]);
        }
        valid[line] = true;
        tags[line] = blockAddr;

        outCycles[0] = 360;                     // 20 check + 340 fill
        return data[line];
    }

    /**
     * Read a single word (for load instructions).
     * Returns the cycle cost and places the value in dest.
     */
    public int readWord(Word32 address, Word32 dest) {
        int addr = TestConverter.toInt(address);
        int blockAddr = addr & ~7;
        int offset = addr & 7;

        for (int i = 0; i < LINES; i++) {
            if (valid[i] && tags[i] == blockAddr) {
                // Hit
                data[i][offset].copy(dest);
                return 20;
            }
        }

        // Miss – fetch block, then return word
        int[] dummy = new int[1];
        Word32[] block = readBlock(blockAddr, dummy);   // fills a line
        block[offset].copy(dest);
        return 360;  // total miss cost
    }

    /**
     * Write a single word (for store instructions). Write‑through.
     * Returns the cycle cost.
     */
    public int writeWord(Word32 address, Word32 value) {
        int addr = TestConverter.toInt(address);
        int blockAddr = addr & ~7;
        int offset = addr & 7;

        // Always write to main memory (write‑through) – parallel, so we assume 0 extra cycles
        writeMemoryWord(addr, value);

        // Look for the block in cache
        for (int i = 0; i < LINES; i++) {
            if (valid[i] && tags[i] == blockAddr) {
                // Hit – update cache, cost 20
                value.copy(data[i][offset]);
                return 20;
            }
        }

        // Miss – we need to allocate the block (write‑allocate)
        int[] dummy = new int[1];
        readBlock(blockAddr, dummy);   // brings block in, cost 360
        // The write itself is included in that 360 (check + fill + write‑through done)
        return 360;
    }

    // Helper – read a word from physical memory (0 cycles – we account for time elsewhere)
    private void readMemoryWord(int wAddr, Word32 dest) {
        Word32 addrObj = new Word32();
        TestConverter.fromInt(wAddr, addrObj);
        addrObj.copy(mainMemory.address);
        mainMemory.read();
        mainMemory.value.copy(dest);
    }

    // Helper – write a word to physical memory
    private void writeMemoryWord(int wAddr, Word32 val) {
        Word32 addrObj = new Word32();
        TestConverter.fromInt(wAddr, addrObj);
        addrObj.copy(mainMemory.address);
        val.copy(mainMemory.value);
        mainMemory.write();
    }
}