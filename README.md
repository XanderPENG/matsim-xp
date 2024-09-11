# matsim-xp

This repository contains models and scenarios using MATSim, developed by @Xander Peng.

## Features

- Network converter: convert raw road network file in some commonly used formats (e.g., shp. osm-pbf, geojson) into multimodal MATSim network.

## TODO

-[ ] Add *Relation* process for OSM data, which is used for the PT network.
- 
-[ ] Complete the interconversion between road network and shp & geojson

-[ ] Develop a MATSim network-based `NetworkCleaner` to process the link length (i.e., merge/split links that are too short/long)

-[ ] Check the reliability of cycling network using the converter. (check the consistence of the OSM network and the ***circulation plan***)

-[ ] A small test with randomly generated vehicles (i.e., FVs and cargo bikes, respectively), and compare the VRP solutions for FVs against bikes with the circulation plan.

-[ ] Explore the combinatorial usage of `emission` and `freight` contribs.

-[ ] How to make others use our code/module easily? Think about the use cases.
