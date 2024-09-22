# matsim-xp

This repository contains models and scenarios using MATSim, developed by **Xander Peng**.

## Features

- Network converter: convert raw road network file in some commonly used formats (e.g., shp. osm-pbf, geojson) into multimodal MATSim network.

## TODO

- [X] Add *Relation* process for OSM data, which is used for the `pt` network.

- [X] Complete the interconversion between road network and shp & geojson

- [X] Develop a MATSim network-based `NetworkOptimizer` to process the link length (i.e., merge/split links that are too short/long); try to use the existing `NetworkSimplifier` to do this.

- [ ] A new `NetworkCleaner` to process the connectivity of the multimodal network. (try to improve the existing `NetworkCleaner`)

- [X] Check the reliability of cycling network using the converter. (check the consistence of the OSM network and the ***circulation plan***)

- [ ] A small test with randomly generated vehicles (i.e., FVs and cargo bikes, respectively), and compare the VRP solutions for FVs against bikes with the circulation plan.

- [ ] Explore the combinatorial usage of `emission` and `freight` contribs.

- [ ] How to make others use our code/module easily? Think about the use cases.

## Notes
- The MATSim `NetworkCleaner` & `MultimodalNetworkCleaner` are quite same; both are based on the *findCluster* and *findBiggestCluster* methods：
  - `findCluster` uses 2 while-loops (forward and backward) to find the connected links of a certain given start node.
  - `findBiggestCluster` uses `findCluster` to find all the clusters in the network, and then find the biggest cluster. It will be terminated when the biggest cluster is found (when the size (number of nodes) of the biggest cluster is larger/equal to the remaining size of the network).
  - For `MultimodalNetworkCleaner`, it is ***link*** based (not node based):
    - only the link's allowed modes are intersected with the specified modes will be put into the cluster.
    - Although there are 2 input parameters (`cleaning modes`&`connectivity modes`) for searching the biggest cluster of the multimodal network, they will be merged as one set when calling the `findCluster` method.
    - However, when the `find biggest cluster` is finished, the remaining links (not in the biggest cluster) will be modified/removed:
      - modified: the link will be kept if it's allowed modes is not empty after removing the `cleaning modes` 
      - removed: the link will be removed if it's allowed modes is empty after removing the `cleaning modes`