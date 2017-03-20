# PXP
Proof of Concept Demonstration of PXP: Policy Exchange Protocol - designed as a solution for the problem of Geo-location-based Access Control in Named Data Networking.

This code was developed as part of my postgraduate thesis - availabe to read at https://cjhill.me/postgraduate-project/

Running the demo requires a compatible version of the [Named Data Forwarding Daemon (NFD)](https://github.com/named-data/NFD) to be operational on the local machine of each Actor's JVM process (both may be run on the same local machine). 
Each Actor - Alice as Owner and Bob as Contractor - must have their NDN Keys preconfigured for signing Interests and Data as "/ndn/pxp/demo/alice" and "/ndn/pxp/demo/bob" respectively.

ContractorDemo and OwnerDemo should then be executed, in that order, from separate terminals as both require some user input.

The version of NFD used and tested against was 0.4.1-1-g704430c, using jNDN version v0.13 (2016-07-20), so ymmv if using a newer build.
Note that SQLite should also be accessable on the local machine.

I'm well aware that setting up the demo is complex, requiring familiarity with NDN and the NFD, however the Java code should be relatively straight forward to understand on its own for anyone interested in better understanding the protocol. The implemention's primary purpose is to complement the design of PXP as described in Chapter 4 of the thesis.
