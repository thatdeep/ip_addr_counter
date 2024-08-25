# Unique IP Counter

This project contains various implementations for counting unique IP addresses from large files. The primary goal is to achieve efficient memory usage and processing speed.

## Implementations

### 1. `UniqueIPCounter.java`
- **Description**: Basic implementation using a `HashSet` to count unique IPs.
- **Result**: Quickly runs out of memory (OOM) when handling large files.

### 2. `UniqueIpCounterInt.java`
- **Description**: Similar to the previous implementation, but each IP is uniquely encoded as an `int32` value.
- **Result**: Still encounters OOM issues with large files.

### 3. `UniqueIpCounterBitArray.java`
- **Description**: 
  - There are 2^32 possible unique IP values. Given the large file size, we expect a significant portion of the int32 range to be present.
  - The most memory-efficient approach is to store the presence of each address in its own bitflag.
  - This results in a bitmask array of 2^32 elements, equivalent to 2^29 bytes (~537MB).
  - The process involves reading lines, converting them to integers, and setting bits at corresponding bit indices.
- **Performance**: Achieved a processing speed of around 220 seconds for a 14 GB partial file.

### 4. `UniqueIPCounterBitArrayParallel.java`
- **Description**: Inspired by the [1 billion row challenge blog post](https://questdb.io/blog/billion-row-challenge-step-by-step/) and their code for parallel file reading/processing in chunks.
  
    - Treat the file as a contiguous memory segment and calculate chunk starting and ending offsets.
    - The bitmask is shared among threads to save memory. Concurrent access is managed by dividing the bitmask into 256 `AtomicIntegerArray` segments, indexed by the first part of the IP address. This reduces the likelihood of concurrent access to the same segment, as the segments have non-overlapping index spaces.
    - Each thread parses its lines within its chunk, converts them to a global bitmask index, determines the segment and segment index, and performs atomic read-modify-update operations until success.
    - The final step is to aggregate the count of bits set to 1, which corresponds to the total number of unique IP addresses.

- **Results**: 
  - Successfully computed 1 billion unique addresses from a test file.
  - Processing time on a MacBook Air M1 was between 140-160 seconds for a 114GB file.
- **Notes**:
  - Utilized JDK 22 to leverage advanced `java.lang.foreign` features