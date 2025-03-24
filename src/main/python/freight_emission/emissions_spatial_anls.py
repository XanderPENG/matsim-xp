import pandas as pd
import geopandas as gpd
import os
import matsim
import logging
import h3
from shapely.geometry import Polygon

import pollutants
import emission_events_anls as emission_anls


def load_city_leuven_boundary(filepath = None):
    if filepath is None:
        filepath = r'../../../../data/intermediate/test/boundary/leuven_census.geojson'
        leuven_boundary = gpd.read_file(filepath)
    else:
        leuven_boundary = gpd.read_file(filepath)
    return leuven_boundary

def create_h3_hexagon_with_boundary(boundary: gpd.GeoDataFrame, resolution: int = 9):
    '''Aggregate the boundary to a single polygon'''
    boundary_aggregated = boundary.unary_union
    boundary_aggregated = gpd.GeoDataFrame(geometry=[boundary_aggregated])
    boundary_aggregated.crs = boundary.crs
    
    '''Convert Shapely polygon to coordinates format required by h3'''
    polygon_coords = [list(coord) for coord in boundary_aggregated.to_crs('EPSG:4326').geometry[0].exterior.coords]
    # Note: h3 expects coordinates as [lat, lng] pairs
    polygon_coords = [[coord[1], coord[0]] for coord in polygon_coords[:-1]]  # Remove last point and swap lat/lng

    h3_polygon = h3.LatLngPoly(polygon_coords)
    # Get h3 hexagons of the boundary
    boundary_hex = h3.polygon_to_cells(h3_polygon, resolution)

    '''Convert the h3 hexagons to geodataframe'''
    hex_polygons = []
    for h in boundary_hex:
        # Get the coordinates of the hexagon boundary
        boundary = h3.cell_to_boundary(h)
        # Convert to shapely polygon
        polygon = Polygon([(lng, lat) for lat, lng in boundary])
        hex_polygons.append({'h3_index': h, 'geometry': polygon})

    # Create GeoDataFrame
    hex_gdf = gpd.GeoDataFrame(hex_polygons)
    hex_gdf.set_crs(epsg=4326, inplace=True)
    hex_gdf.to_crs(epsg=31370, inplace=True)

    return hex_gdf

def spatial_join_with_h3_hexagons(network_gdf: gpd.GeoDataFrame, 
                                  hex_gdf: gpd.GeoDataFrame,
                                  output_dir: str,
                                  resolution: int,
                                  anls_cols: list,
                                  ):
            
    hex_emissions_gdf = gpd.sjoin(hex_gdf, network_gdf, how='left', predicate='intersects')
    # Aggregate the emissions

    hex_emissions_gdf = hex_emissions_gdf.groupby('h3_index')[anls_cols].sum()
    hex_emissions_gdf.reset_index(inplace=True)

    hex_gdf = hex_gdf.merge(hex_emissions_gdf, on='h3_index', how='left')
    # Only retain hexagons with emissions
    hex_gdf = hex_gdf[hex_gdf['air_quality_pollutants'] > 0]

    ''' Write the emissions to csv '''
    hex_gdf.to_file(output_dir + f'h3Res{resolution}.geojson', driver='GeoJSON')
    logging.info(f'Finished writing emissions: h3Res{resolution}.geojson')


if __name__ == '__main__':

    logging.basicConfig(level=logging.INFO)
    iter_list = list(range(300, 400))
    # iter_index = 100
    scenario_type_list = ['Basic', 'CB', 'Van']

    for iter_index in iter_list:
        logging.info('Starting the emissions spatial analysis')
        logging.warning(f'Iter index: {iter_index}')

        ''' Assign the emissions to the network links '''
        for scenario_type in scenario_type_list:
            logging.info('Processing scenario: ' + scenario_type)
            events_output_dir = r'../../../../data/intermediate/test/freightEmissions/scenario' + scenario_type + '/iter'+ str(iter_index) + '/outputs/'
            shp_output_dir = r'../../../../data/clean/freightEmissions/shp/' + scenario_type.lower() + '/' + str(iter_index) + '/'
            os.makedirs(shp_output_dir, exist_ok=True)

            ''' Read the scenario network '''
            scenario_network = matsim.read_network(events_output_dir + 'output_network.xml.gz')
            scenario_network_gdf = scenario_network.as_geo(projection='EPSG:31370')

            '''Emissions events'''
            emissions_events_df = emission_anls.read_freight_emission_data(events_output_dir, 1)
            emissions_on_link_by_type_dict = emission_anls.get_all_pollutants_by_link_dict(emissions_events_df)

            for pollutant, emissions_on_link_df in emissions_on_link_by_type_dict.items():
                if emissions_on_link_df is None:
                    logging.warning(f'No emissions for pollutant: {pollutant}')
                    continue
                _emission_df = emissions_on_link_df.reset_index()[['linkId', 'sum']]
                _emission_df.columns = ['link_id', pollutant]
                # _emission_df['linkId'] = _emission_df['linkId'].astype(str)
                scenario_network_gdf = pd.merge(scenario_network_gdf, _emission_df, how='left', on='link_id')

            # Sum air quality pollutants
            scenario_network_gdf['air_quality_pollutants'] = 0
            for pollutant in pollutants.AIR_QUALITY_POLLUTANTS:
                if pollutant in scenario_network_gdf.columns:
                    scenario_network_gdf['air_quality_pollutants'] += scenario_network_gdf[pollutant]
                else:
                    logging.warning(f'No pollutant:, {pollutant}')

            anls_cols = pollutants.POLLUTANTS
            anls_cols.add('air_quality_pollutants')
            anls_cols = list(anls_cols)
            # Remove pollutant that is not in the network columns 
            anls_cols = [col for col in anls_cols if col in scenario_network_gdf.columns]

            """ Spatial join with the spatial analysis units """
            ''' using h3 hexagons '''
            # Resolution 9
            leuven_boundary = load_city_leuven_boundary()
            hex_gdf = create_h3_hexagon_with_boundary(leuven_boundary, resolution=9)
            spatial_join_with_h3_hexagons(scenario_network_gdf, hex_gdf, shp_output_dir, 9, anls_cols)
            
            # Resolution 10
            hex_gdf = create_h3_hexagon_with_boundary(leuven_boundary, resolution=10)
            spatial_join_with_h3_hexagons(scenario_network_gdf, hex_gdf, shp_output_dir, 10, anls_cols)

            ''' using census tracts '''
            # # Spatial join with census tracts
            # joined_census_tracts = gpd.sjoin(leuven_boundary, scenario_network_gdf, how='left', predicate='intersects')
    
            # joined_census_tracts = joined_census_tracts.groupby('cd_sector')[anls_cols].sum()
            # joined_census_tracts.reset_index(inplace=True)

            # merged_census_tracts = leuven_boundary.merge(joined_census_tracts, on='cd_sector', how='left')
            # merged_census_tracts.fillna(0, inplace=True)
            
            # ''' Write the census tracts '''
            # merged_census_tracts.to_file(shp_output_dir + 'census_tracts.geojson', driver='GeoJSON')

