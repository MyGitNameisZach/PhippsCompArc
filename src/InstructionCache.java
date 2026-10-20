public class InstructionCache {
    private boolean valid = false;
    private int tag = -1;                     // block address (word address of first word)
    private Word32[] data = new Word32[8];    // 8‑word cache line

    // Next memory level – must implement readBlock(int blockAddr)
    private L2Cache nextLevel;                // or Memory directly if no L2

    public InstructionCache(L2Cache next) {
        this.nextLevel = next;
        for (int i = 0; i < 8; i++) data[i] = new Word32();
    }

    /**
     * Read one instruction word.
     * @param address  Word32 address to read
     * @param dest     where to put the result
     * @return cycles used for this read (10 hit, 30 L2 hit, 370 L2 miss)
     */
    public int read(Word32 address, Word32 dest) {
        int addr = TestConverter.toInt(address);
        int blockAddr = addr & ~7;    // integer division truncation, works for positive addresses
        int offset = addr & 7;

        if (valid && tag == blockAddr) {
            // Cache hit
            data[offset].copy(dest);
            return 10;                  // 10 cycles for I‑cache hit
        }

        // Miss – fetch the block from nextLevel
        int[] l2Cycles = new int[1];   // out‑parameter
        Word32[] block = nextLevel.readBlock(blockAddr, l2Cycles);

        // Fill local cache
        for (int i = 0; i < 8; i++) {
            block[i].copy(data[i]);
        }
        valid = true;
        tag = blockAddr;

        // Return data + cycles
        data[offset].copy(dest);
        return 10 + l2Cycles[0];       // 10 (check) + L2 time
    }
}