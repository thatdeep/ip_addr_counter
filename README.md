UniqueIPCounter.java

basic hashset line counting, OOM very fast

UniqueIpCounterInt.java

same hashset counting, but each ip is encoded as int32 value uniquely. Still OOM hard.

UniqueIpCounterBitArray.java

There are 2^32 unique IP values, and file is large so we can expect large portion of whole int32 range present. Best we can do in terms of memory is storing presence of each address in its own flag. This leads to 2^32 elements bitmask array, which translates to 2^29 bytes (~537MB array). Rest is straightforward: read lines, make integers, set bits at corresponding bit indices. Speed was around 220 sec per 14 GB partial file.

UniqueIPCounterBitArrayParallel.java

{/strikethrough}{stole from}inspired by 1 billion row challenge blogpost https://questdb.io/blog/billion-row-challenge-step-by-step/ and their code for parallel file reading/processing in chunks.

Basically:
1) view file as contiguous memory segment, compute and fix chunks starting and ending offsets.
2) bitmask is shared between threads. Concurrent access is achieved with looking at bitmask as 256 AtomicIntegerArray segments, segments indexed by first subaddress part. This way probability of concurrent access to the same AtomicIntegerArray is reduced as segments have non-overlapping indices spaces.
3) each thread parse its lines within chunk, converts them to global bitmask index, then determine segment and segment index from it. Then repeat atomic read-modify-update thing until success.
4) Aggregate count of bits set to 1, which will correspond to total unique ip addresses.

Results: 1 billion unique addresses for a given test file, were computed on macbook air M1 within 140-160s, which feels quite good for a 114GB file. Used jdk22 to use those fancy java.lang.foreign things