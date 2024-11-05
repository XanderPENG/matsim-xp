# matsim-xp

This repository contains models and scenarios using MATSim, developed by **Xander Peng**.

## Features

- Network converter: convert raw road network file in some commonly used formats (e.g., shp. osm-pbf, geojson) into multimodal MATSim network.
- Combinatorial implementation of both `emissions` and `freight` modules.

## TODO

- [X] Add *Relation* process for OSM data, which is used for the `pt` network.

- [X] Complete the interconversion between road network and shp & geojson

- [X] Develop a MATSim network-based `NetworkOptimizer` to process the link length (i.e., merge/split links that are too short/long); try to use the existing `NetworkSimplifier` to do this.

- [X] A new `NetworkCleaner` to process the connectivity of the multimodal network. (try to improve the existing `NetworkCleaner`)

- [X] Check the reliability of cycling network using the converter. (check the consistence of the OSM network and the ***circulation plan***)

- [X] A small test with randomly generated vehicles (i.e., FVs and cargo bikes, respectively), and compare the VRP solutions for FVs against bikes with the circulation plan.

- [X] Explore the combinatorial usage of `emission` and `freight` contribs.

- [ ] How to make others use our code/module easily? Think about the use cases.
