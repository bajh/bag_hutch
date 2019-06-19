# Bag Hutch

Bag Hutch is a crude attempt to implement the [Dynamo DB paper](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf) for learning and fun. The storage backend is [MapSack](https://github.com/bajh/mapsack), an equally crude [Bitcask](https://riak.com/assets/bitcask-intro.pdf) implementation.

[![Bag Hutch](http://img.youtube.com/vi/6gdAJefPME4/0.jpg)](http://www.youtube.com/watch?v=6gdAJefPME4)

"Bags must be folded neatly"

#Features

**Consistent Hashing and Query Routing**

**Consistency/Availability Configuration** The n, r, and w parameters allow consistency to be sacrificed for availability or vice versa.

**Vector Clocks** Vector clocks identify divergences in the data stored on key replicas and allow those divergences to be reconciled, when possible, and surfaced to the client.

**Gossip Protocol** [SWIM](https://www.cs.cornell.edu/projects/Quicksilver/public_pdfs/SWIM.pdf) gossip protocol is used to identify node failures and circulate liveness information throughout the cluster

#TODOs

**Transaction Rollback** When a new write enters the system, it is first routed to a node that can write the data locally. After the vector clock is incremented for that node and the data is committed locally, the value and its updated clock are submitted to the other nodes that need to take part in the write, which succeeds only if w nodes in total successfully write the message. I'm not clear what is meant to happen if writing to the first node is unsuccessful (or if writing to some of the other nodes succeeeds but fewer than w total succeed) - presumably there's a way for the nodes to guarantee they can roll back what has already been written?

**Code Improvement** In this first effort, my goal was to understand different data structures and distributed systems concepts, and get better at Java, rather than to create a usable piece of software. In particular, efficiency and concurrency concerns were given second shrift to understanding the core concepts. It would interesting to do a rewrite, perhaps using Akka, that focuses more on performance and correctness.

**Tests**

**vNodes** Currently each node in the cluster is assigned a single segment of the ring, whereas in DynamoDB and Riak locate multiple "virtual nodes" on each physical node. It is preferable to break the ring up into smaller segments, because (I think) this allows data to be handed off to new nodes that enter the cluster more easily.

**Hinted Handoff** Right now nodes are assigned ring tokens at startup time and entering and leaving the cluster is not supported. Hinted handoff should be implemented to allow partitions to be transferred from leaving nodes and to entering nodes.

**Anti-Entropy** In Riak, conflicts are actively identified using merkle trees, which allow multiple nodes to locate conflicts by exchanging hashes. Some aspects of how this works are unclear to me, which makes me think a larger structural change may need to take place (right now, no two nodes have the same data, so I'm not yet clear what groups of data the merkle trees are build off of).

**Map-Reduce** BagHutch can only retrieve single values by key. It would be fun to implement a map-reduce query language to allow aggregate views of multiple keys to be returned.

**SWIM** suspect node confirmation mechanism. According to the "more robust and efficient SWIM" described in the paper, nodes that suspect another node of being faulty set an additional timeout after which they confirm its failure if they don't receive a contradictory message circulated by that node.
