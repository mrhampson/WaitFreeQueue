# WFQueue

An implementation of a wait free queue in Java based on this paper from Kogan and Petrank http://www.cs.technion.ac.il/~erez/Papers/wfquque-ppopp.pdf

## Challenges from adapting algorithm from the paper
- One of the difficult parts of implementing this from the paper is finding a way to automatically map a thread to an index in the queue's operation state array. In the paper they just have each thread uniquely numbered from 1 to N so they can directly assign it to an index in the state array. At first I thought about creating a map from `Thread.currentThread().getId()` to an index in the state array but then I realized I would have to synchronize access to the map somehow so the map would no longer be wait-free (or lock-free). So to map the java thread id to an index in the state array I added a parralel `AtomicLongArray threadIdArray` that would store the java thread id as a value. This way when a thread calls enqueue I can find it's index in the state array by iterating through `threadIdArray` and look for the thread id in this array or add the thread id to the array if there's an empty slot. 
