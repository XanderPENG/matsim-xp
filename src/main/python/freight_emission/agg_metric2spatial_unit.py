import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matsim
import geopandas as gpd
from pyproj import Geod
import os
import pollutants
import networkx as nx
from haversine import haversine_vector, Unit

scenarios_gdf = {}
scenarios_emissions = {}

def load_scen_emissions_gdf(iter_list, root_path, scenario_types, h3_res):
    
    for scenario in scenario_types:
        scenario_unit_gdf = gpd.GeoDataFrame()  # Create an empty GeoDataFrame to store only the geometries
        scenario_unit_emissions = {}
        for i in iter_list:
            print('Processing scenario: ' + scenario + ' iteration: ' + str(i))
            # Load the data
            iter_gdf = gpd.read_file(os.path.join(root_path, scenario.lower(), str(i), f'h3res{h3_res}.geojson'))

            ''' Concatenate the geometries of the current iteration to the scenario GeoDataFrame '''
            scenario_unit_gdf = pd.concat([scenario_unit_gdf, iter_gdf[['h3_index', 'geometry']]])
            scenario_unit_gdf = scenario_unit_gdf.drop_duplicates(subset=['h3_index'])
            
            ''' Get the detailed emissions for each spatial unit '''
            for idx, row in iter_gdf.iterrows():
                unit_idx = row['h3_index']
                detailed_emissions = (row[list(filter(lambda x: x not in ['h3_index', 'geometry'],iter_gdf.columns))]).to_dict()

                if scenario_unit_emissions.get(unit_idx) is None:  # If this is the first time we encounter this spatial unit
                    scenario_unit_emissions[unit_idx] = {i: detailed_emissions}
                else:  # If this is not the first time we encounter this spatial unit, add the emissions to the existing dictionary
                    scenario_unit_emissions[unit_idx][i] = detailed_emissions

        scenarios_gdf[scenario] = scenario_unit_gdf
        scenarios_emissions[scenario] = scenario_unit_emissions

    return scenarios_gdf, scenarios_emissions

def cal_new_indicator(emission_df: pd.DataFrame, indicator_info: dict):
    copy_df = emission_df.copy(deep=True)
    # Calculate the new indicator
    for indicator, emissions_and_weights in indicator_info.items():
        filtered_emissions_and_weights = {k: v 
                                          for k, v in emissions_and_weights.items() if k in copy_df.iloc[0, 0].keys()}
        filtered_emissions = set(emissions_and_weights.keys()).difference(set(filtered_emissions_and_weights.keys()))
        if len(filtered_emissions) > 0:
            print(f" {filtered_emissions} is/are not found in the emissions df, and thus will not be considered in the calculation of the indicator")
        copy_df[indicator+'_mean'] = copy_df.apply(lambda x: np.mean([np.sum([x[i][sub_emission] * weight 
                                                                              for sub_emission, weight in filtered_emissions_and_weights.items()])
                                                                     for i in iter_list if isinstance(x[i], dict)]), 
                                                   axis=1)
        copy_df[indicator+'_median'] = copy_df.apply(lambda x: np.median([np.sum([x[i][sub_emission] * weight
                                                                                  for sub_emission, weight in filtered_emissions_and_weights.items()])
                                                                         for i in iter_list if isinstance(x[i], dict)]), 
                                                     axis=1) 

    return copy_df

def stats_unit(agg_emission_df: pd.DataFrame,
              spatial_unit_gdf: gpd.GeoDataFrame,
              emissions: list,  # A list of Emission component that we want to calculate the statistics for
              new_stats_index: dict = None,  # A dictionary of new statistics to be calculated {str'index_name': dict{sub_emission:weight}}
            #   stat_type: str,  # Statistic type: Mean or Median
              ):
    # assert stat_type.lower() in ['mean', 'median', 'both'], 'Invalid statistic type. Please choose between Mean and Median'
    assert isinstance(emissions, list), 'Pollutant must be a list'

    copy_agg_emission_df = agg_emission_df.copy(deep=True)
    copy_spatial_unit_gdf = spatial_unit_gdf.copy(deep=True)

    for e in emissions:
        assert e in pollutants.POLLUTANTS or e == 'air_quality_pollutants', 'Invalid pollutant'
        if e not in agg_emission_df.iloc[0,0].keys():
            print(f'{e} is not in the emissions dictionary. Skipping...')
            continue
        copy_agg_emission_df[e+'_mean'] = agg_emission_df.apply(lambda x: np.mean([x[i][e] 
                                                            for i in iter_list if isinstance(x[i], dict)]
                                                            ), 
                                            axis=1)

        copy_agg_emission_df[e+'_median'] = agg_emission_df.apply(lambda x: np.median([x[i][e] 
                                                            for i in iter_list if isinstance(x[i], dict)]
                                                            ), 
                                            axis=1)
    
        ''' Merge the statistics with the spatial unit GeoDataFrame '''
        copy_spatial_unit_gdf = pd.merge(copy_spatial_unit_gdf, copy_agg_emission_df[[e+'_mean', e+'_median']],
                                         left_on='h3_index', right_index=True)
    
    ''' Statistics for new indicators '''
    if new_stats_index is not None:
        new_indicator_agg_emission_df = cal_new_indicator(agg_emission_df, new_stats_index)
        for indicator in new_stats_index.keys():
            copy_spatial_unit_gdf = pd.merge(copy_spatial_unit_gdf, new_indicator_agg_emission_df[[indicator+'_mean', indicator+'_median']],
                                             left_on='h3_index', right_index=True)


    return copy_spatial_unit_gdf
    
''' Utils for road network metrics '''

def agg_unit_network_links_nodes(spatial_unit_gdf: gpd.GeoDataFrame, network_gdf: gpd.GeoDataFrame, nodes_gdf: gpd.GeoDataFrame):
    
    new_gdf = spatial_unit_gdf.copy(deep=True)
    
    ''' agg links '''
    sjoin_links = gpd.sjoin(new_gdf, network_gdf, how='left', predicate='intersects')
    link_stats = sjoin_links.groupby('h3_index').agg({'link_id': 'count'}).reset_index().rename(columns={'link_id': 'link_count'})
    # Merge the statistics with the spatial unit GeoDataFrame
    new_gdf = pd.merge(new_gdf, link_stats, on='h3_index', how='left')

    ''' agg nodes '''
    sjoin_nodes = gpd.sjoin(new_gdf, nodes_gdf, how='left', predicate='contains')
    node_stats = sjoin_nodes.groupby('h3_index').agg({'node_id': 'count'}).reset_index().rename(columns={'node_id': 'node_count'})
    # Merge the statistics with the spatial unit GeoDataFrame
    new_gdf = pd.merge(new_gdf, node_stats, on='h3_index', how='left')

    return new_gdf

# Convert network to graph
''' Convert network to graph '''
def getNodeInfo(nodesDict,
                nodeCoords,
                ):
    nodesDictKeys = list(nodesDict.keys())
    nodesDictValues = list(nodesDict.values())

    node_id = None
    if nodesDict == {}:
        return node_id

    # If the node is in the nodeDict
    if nodeCoords in nodesDictValues:
        node_id = nodesDictKeys[nodesDictValues.index(nodeCoords)]

    else:  # Find the 'same' node in the nodeDict (distance < 0.2m)
        dist_array = haversine_vector(np.array(nodesDictValues),
                                      np.array([nodeCoords]),
                                      Unit.METERS,
                                      comb=True)
        # Get the index of the 'same' node (if applicable)
        try:
            closeNode_idx = np.where(dist_array < 0.2)[1][0]
        except:
            closeNode_idx = None

        # If the point is close to an existing node, return the node id of the existing node
        if closeNode_idx is not None:
            node_id = nodesDictKeys[closeNode_idx]

    return node_id

def create_graph(road_gdf: gpd.GeoDataFrame,
                 length_col='length',
                 
                 ):
    splitG = nx.DiGraph()

    # Create a dictionary to store the unique nodes (id, coords)
    nodesDict = {}

    start_nodes = []
    end_nodes = []

    node_idx = 0
    for idx, row in road_gdf.iterrows():
        # idx = row.index
        length = row[length_col]
        line = row['geometry']
        line_orig_id = row['link_id']

        # (lat, lon) format
        lineStartCoords = (line.coords[0][1], line.coords[0][0])
        lineEndCoords = (line.coords[-1][1], line.coords[-1][0])

        # Get the node id of the start and end nodes
        startNodeOrigId = row['from_node']
        endNodeOrigId = row['to_node']

        startNode_id = getNodeInfo(nodesDict, lineStartCoords)
        if startNode_id is None:
            startNode_id = node_idx
            node_idx += 1
            nodesDict[startNode_id] = lineStartCoords

        endNode_id = getNodeInfo(nodesDict, lineEndCoords)
        if endNode_id is None:
            endNode_id = node_idx
            node_idx += 1
            nodesDict[endNode_id] = lineEndCoords

        start_nodes.append(startNode_id)
        end_nodes.append(endNode_id)

        # Add nodes to the graph
        if not splitG.has_node(startNode_id):
            splitG.add_node(startNode_id, x=lineStartCoords[1], y=lineStartCoords[0], 
                            orig_id=startNodeOrigId # Add the original node id of the node
                            )
        if not splitG.has_node(endNode_id):
            splitG.add_node(endNode_id, x=lineEndCoords[1], y=lineEndCoords[0],
                            orig_id=endNodeOrigId # Add the original node id of the node
                            )
        splitG.add_edge(startNode_id, endNode_id,
                        length=length,
                        orig_id=line_orig_id,
                        idx=idx)

    splitG.graph['crs'] = "EPSG:4326"

    indexed_road = road_gdf.copy()

    indexed_road['start_node'] = start_nodes
    indexed_road['end_node'] = end_nodes

    return splitG, indexed_road

def identify_intersections(graph: nx.DiGraph):
    intersections = {}

    for node in graph.nodes:
        
        if len(list(graph.neighbors(node))) >= 3:
            intersections[graph.nodes[node]['orig_id']] = list(graph.neighbors(node))
    intersection_df = pd.DataFrame(intersections.items(), columns=['orig_node_id', 'neighbors'])

    return intersection_df

def agg_unit_network_intersections(spatial_unit_gdf: gpd.GeoDataFrame, intersections_gdf: gpd.GeoDataFrame):
    new_gdf = spatial_unit_gdf.copy(deep=True)
    
    ''' agg intersections '''
    sjoin_intersections = gpd.sjoin(new_gdf, intersections_gdf, how='left', predicate='contains')
    intersection_stats = sjoin_intersections.groupby('h3_index').agg({'orig_node_id': 'count'}).reset_index().rename(columns={'orig_node_id': 'intersection_count'})
    # Merge the statistics with the spatial unit GeoDataFrame
    new_gdf = pd.merge(new_gdf, intersection_stats, on='h3_index', how='left')
    new_gdf['intersections_ratio'] = new_gdf['intersection_count'] / new_gdf['node_count']
    return new_gdf

def geodesic_area(geom):
    geod = Geod(ellps="WGS84")
    if geom.is_empty:
        return np.nan
    try:
        area, _ = geod.geometry_area_perimeter(geom)
        return abs(area) / (1000 * 1000)
    except Exception as e:
        print(f"fail: {e}")
        return np.nan
    
def cal_unit_network_density(spatial_unit_gdf: gpd.GeoDataFrame, network_gdf: gpd.GeoDataFrame):
    new_gdf = spatial_unit_gdf.copy(deep=True)
    if 'area' not in new_gdf.columns:
        new_gdf['area'] = new_gdf['geometry'].area / (1000 * 1000)  # Convert to km2
    ''' Calculate the density of the network '''
    sjoin_links = gpd.sjoin(new_gdf, network_gdf, how='left', predicate='intersects')
    link_stats = sjoin_links.groupby('h3_index').agg({'length': 'sum'}).reset_index().rename(columns={'length': 'link_length'})
    # Merge the statistics with the spatial unit GeoDataFrame
    new_gdf = pd.merge(new_gdf, link_stats, on='h3_index', how='left')
    new_gdf['link_length'] = new_gdf['link_length'] / 1000  # Convert to km
    
    new_gdf['network_density'] = new_gdf['link_length'] / new_gdf['area']

    return new_gdf

# Depots utils
''' Calculate the distance between the spatial unit centroid and each depot using gpd distance
    , and return the matrix '''
def cal_distance_matrix(unit_gdf: gpd.GeoDataFrame, depot_gdf: gpd.GeoDataFrame):
    distance_matrix = np.zeros((len(unit_gdf), len(depot_gdf)))
    for i, unit_centroid in enumerate(unit_gdf['geometry'].centroid):
        for j, depot in enumerate(depot_gdf['geometry']):
            distance_matrix[i, j] = unit_centroid.distance(depot)
    
    distance_matrix = pd.DataFrame(distance_matrix, index=unit_gdf['h3_index'], columns=depot_gdf['node_id'])
    
    distance_matrix['mean_distance'] = distance_matrix.mean(axis=1) / 1000  # Convert to km
    distance_matrix['median_distance'] = distance_matrix.median(axis=1) / 1000  # Convert to km
    distance_matrix = distance_matrix.reset_index()
    return distance_matrix

def agg_depots_proximity(spatial_unit_gdf: gpd.GeoDataFrame, depots_gdf: gpd.GeoDataFrame):
    new_gdf = spatial_unit_gdf.copy(deep=True)
    distance_matrix = cal_distance_matrix(new_gdf, depots_gdf)
    new_gdf = pd.merge(new_gdf, distance_matrix[['h3_index', 'mean_distance', 'median_distance']], on='h3_index')
    return new_gdf

def count_depots_in_unit(spatial_unit_gdf: gpd.GeoDataFrame, depots_gdf: gpd.GeoDataFrame):
    new_gdf = spatial_unit_gdf.copy(deep=True)
    sjoin_depots = gpd.sjoin(new_gdf, depots_gdf, how='left', predicate='contains')
    depot_stats = sjoin_depots.groupby('h3_index').agg({'node_id': 'count'}).reset_index().rename(columns={'node_id': 'depot_count'})
    # Merge the statistics with the spatial unit GeoDataFrame
    new_gdf = pd.merge(new_gdf, depot_stats, on='h3_index', how='left')
    return new_gdf

''' centrality utils '''
def read_centrality(centrality_root: str, centrality_types: list, scenario: str):
    agg_centralities = {'node-based': None, 'link-based': None}
    if scenario.lower() == "cb":
        folder_kw = 'bikeNetwork'
    elif scenario.lower() == "van":
        folder_kw = 'carNetwork'
    elif scenario.lower() == "basic":
        folder_kw = 'carNetwork'
    else:
        raise ValueError('Invalid scenario name')
    
    for centrality_type in centrality_types:
        centrality_df = pd.read_csv(centrality_root + folder_kw + '//' + centrality_type + '.csv.gz', index_col=0)
        if centrality_type == 'edges_betweeness':
            agg_centralities['link-based'] = centrality_df
        else:
            if agg_centralities['node-based'] is None:
                agg_centralities['node-based'] = centrality_df
            else:
                agg_centralities['node-based'] = pd.merge(agg_centralities['node-based'], centrality_df, on='orig_id', how='left')
    agg_centralities['node-based'] = agg_centralities['node-based'].drop(columns=['node_x', 'node_y', 'node_id'])
    return agg_centralities

def agg_centralities(agg_centrality_dict: dict, 
                     nodes_gdf: gpd.GeoDataFrame,
                     links_gdf: gpd.GeoDataFrame,
                     scenario_stat_gdf: gpd.GeoDataFrame,
                     ):
    new_gdf = scenario_stat_gdf.copy(deep=True)

    nodes_with_centrality = nodes_gdf.merge(agg_centrality_dict['node-based'], left_on='node_id', right_on='orig_id', how='left')
    nodes_with_centrality = nodes_with_centrality.dropna()

    node_sjoin_stat = gpd.sjoin(new_gdf, nodes_with_centrality, how='left', predicate='contains')
    node_centrality_stat = node_sjoin_stat.groupby('h3_index')[['degree_centrality', 'closeness', 'eigenvector']].mean().reset_index()

    links_with_centrality = links_gdf.merge(agg_centrality_dict['link-based'], left_on='link_id', right_on='orig_id', how='left')
    links_with_centrality = links_with_centrality.dropna()

    link_sjoin_stat = gpd.sjoin(new_gdf, links_with_centrality, how='left', predicate='intersects')
    link_centrality_stat = link_sjoin_stat.groupby('h3_index')[['betweeness']].mean().reset_index()

    ''' merge stats with the scenario stat gdf '''
    new_gdf = pd.merge(new_gdf, node_centrality_stat, on='h3_index', how='left')
    new_gdf = pd.merge(new_gdf, link_centrality_stat, on='h3_index', how='left')

    return new_gdf

if __name__ == "__main__":
    iter_list = list(range(300, 400))
    root_path = r'../../../../data/clean/freightEmissions/shp/'
    scenario_types = ['Basic', 'CB', 'Van']
    h3_res = 10

    # Load the emissions with gdf
    scenarios_gdf, scenarios_emissions = load_scen_emissions_gdf(iter_list, root_path, scenario_types, h3_res)

    ''' Calculate the statistics for each scenario '''
    scenario_stats_gdf = {}
    for scenario in scenario_types:
        scenario_stats_gdf[scenario] = stats_unit(
            agg_emission_df=pd.DataFrame(scenarios_emissions[scenario]).T,
            spatial_unit_gdf=scenarios_gdf[scenario],
            emissions=[
                pollutants.CO,
                pollutants.SO2,
                pollutants.NOx,
                pollutants.NO2,
                pollutants.PM2_5,
                pollutants.PM2_5_non_exhaust,
                pollutants.PM,
                pollutants.PM_non_exhaust,
                "air_quality_pollutants",
            ],
            new_stats_index={
                "EPI": {
                    pollutants.CO: 2.9,
                    pollutants.SO2: 2.9,
                    pollutants.NOx: 5.9,
                    pollutants.NO2: 5.9,
                    pollutants.PM2_5: 38.2,
                    pollutants.PM2_5_non_exhaust: 38.2,
                },
                "weighted_AQI": {
                    pollutants.SO2: 1,
                    pollutants.NO2: 2.5,
                    pollutants.PM: 5,
                    pollutants.PM_non_exhaust: 5,
                    pollutants.PM2_5: 10,
                    pollutants.PM2_5_non_exhaust: 10,
                },
                "PM_total": {
                    pollutants.PM: 1,
                    pollutants.PM_non_exhaust: 1,
                },
                "PM2_5_total": {
                    pollutants.PM2_5: 1,
                    pollutants.PM2_5_non_exhaust: 1,
                },
            },
        )
    
    ''' Stats of road network metrics with spatial units '''
    # Read the road network
    network_root_path = r'../../../../data/intermediate/test/freightEmissions/' 

    bike_network = matsim.read_network(network_root_path + 'bikeGemeenteLeuvenWithHbefaType.xml.gz')
    car_network = matsim.read_network(network_root_path + 'carGemeenteLeuvenWithHbefaType.xml.gz')

    bike_network_gdf = bike_network.as_geo()
    car_network_gdf = car_network.as_geo()

    bike_network_gdf.crs = 'epsg:31370'
    car_network_gdf.crs = 'epsg:31370'

    bike_nodes = bike_network.nodes
    car_nodes = car_network.nodes

    bike_nodes_gdf = gpd.GeoDataFrame(bike_nodes, geometry=gpd.points_from_xy(bike_nodes.x, bike_nodes.y))
    bike_nodes_gdf.crs = 'epsg:31370'
    car_nodes_gdf = gpd.GeoDataFrame(car_nodes, geometry=gpd.points_from_xy(car_nodes.x, car_nodes.y))
    car_nodes_gdf.crs = 'epsg:31370'

    # Output the road network and nodes to geojson files
    # bike_network_gdf.to_file(r'../../../../data/clean/freightEmissions/shp/bike_network.geojson', driver='GeoJSON')
    # car_network_gdf.to_file(r'../../../../data/clean/freightEmissions/shp/car_network.geojson', driver='GeoJSON')   
    # bike_nodes_gdf.to_file(r'../../../../data/clean/freightEmissions/shp/bike_nodes.geojson', driver='GeoJSON')
    # car_nodes_gdf.to_file(r'../../../../data/clean/freightEmissions/shp/car_nodes.geojson', driver='GeoJSON')

    # Convert to graph
    for scenario, stats_gdf in scenario_stats_gdf.items():
        if scenario == "CB":
            network_gdf = bike_network_gdf
            nodes_gdf = bike_nodes_gdf
        else:
            network_gdf = car_network_gdf
            nodes_gdf = car_nodes_gdf
        scenario_stats_gdf[scenario] = agg_unit_network_links_nodes(stats_gdf, network_gdf, nodes_gdf)
    
    bike_splitG, bike_indexed_road = create_graph(bike_network_gdf.to_crs('EPSG:4326'))
    car_splitG, car_indexed_road = create_graph(car_network_gdf.to_crs('EPSG:4326'))

    # Identify intersections
    bike_intersections = identify_intersections(bike_splitG)
    bike_intersections_gdf = bike_intersections.merge(bike_nodes, left_on='orig_node_id', right_on='node_id', how='left')
    bike_intersections_gdf = gpd.GeoDataFrame(bike_intersections_gdf, geometry=gpd.points_from_xy(bike_intersections_gdf.x, bike_intersections_gdf.y))
    bike_intersections_gdf.crs = 'epsg:31370'
    bike_intersections_gdf = bike_intersections_gdf[['orig_node_id', 'neighbors', 'geometry']]
    
    car_intersections = identify_intersections(car_splitG)
    car_intersections_gdf = car_intersections.merge(car_nodes, left_on='orig_node_id', right_on='node_id', how='left')
    car_intersections_gdf = gpd.GeoDataFrame(car_intersections_gdf, geometry=gpd.points_from_xy(car_intersections_gdf.x, car_intersections_gdf.y))
    car_intersections_gdf.crs = 'epsg:31370'
    car_intersections_gdf = car_intersections_gdf[['orig_node_id', 'neighbors', 'geometry']]
    
    # Aggregate the intersections with spatial units
    for scenario, stats_gdf in scenario_stats_gdf.items():
        if scenario == "CB":
            intersections_gdf = bike_intersections_gdf
        else:
            intersections_gdf = car_intersections_gdf
        scenario_stats_gdf[scenario] = agg_unit_network_intersections(stats_gdf, intersections_gdf)
    
    # Calculate and Aggregate the network density with spatial units
    for scenario, stats_gdf in scenario_stats_gdf.items():
        if scenario == "CB":
            network_gdf = bike_network_gdf
        else:
            network_gdf = car_network_gdf
        scenario_stats_gdf[scenario] = cal_unit_network_density(stats_gdf, network_gdf)

    ''' Depots'''
    depots_gdf = gpd.read_file(r'../../../../data/clean/freightEmissions/shp/depots/depots.shp')

    # Depots proximity
    for scenario, stats_gdf in scenario_stats_gdf.items():
        scenario_stats_gdf[scenario] = agg_depots_proximity(stats_gdf, depots_gdf)
    
    # Depots count
    for scenario, stats_gdf in scenario_stats_gdf.items():
        scenario_stats_gdf[scenario] = count_depots_in_unit(stats_gdf, depots_gdf)

    ''' Centrality '''
    centrality_root = r'../../../../data/clean/freightEmissions/networkCentrality/'
    centralities_list = ['degreeCentrality', 'closeness_centrality', 'edges_betweeness', 'eigenvector_centrality']

    for scenario, stats_gdf in scenario_stats_gdf.items():
        centrality_dict = read_centrality(centrality_root, centralities_list, scenario)
        if scenario.lower() == 'cb':
            nodes_gdf = bike_nodes_gdf
            links_gdf = bike_network_gdf
        else:
            nodes_gdf = car_nodes_gdf
            links_gdf = car_network_gdf

        scenario_stats_gdf[scenario] = agg_centralities(centrality_dict, nodes_gdf, links_gdf, stats_gdf)

    # Scaling the centrality metrics
    for scenario, stats_gdf in scenario_stats_gdf.items():
        stats_gdf['degree_centrality'] = stats_gdf['degree_centrality'] * 1e5
        stats_gdf['closeness'] = stats_gdf['closeness'] * 1e5
        stats_gdf['eigenvector'] = (stats_gdf['eigenvector'] - stats_gdf['eigenvector'].min()) / (stats_gdf['eigenvector'].max() - stats_gdf['eigenvector'].min())
        stats_gdf['betweeness'] = stats_gdf['betweeness'] * 1e5
    
    ''' Output the statistics to geojson files '''
    for scenario, stats_gdf in scenario_stats_gdf.items():
        """
        V1: add EPI and AQI
        V2: add PM_total, PM2_5_total
        """
        stats_gdf.to_file(r'../../../../data/clean/freightEmissions/shp/stats/' + scenario + '_stats_h3Res' + str(h3_res) +'V2.geojson', driver='GeoJSON')

