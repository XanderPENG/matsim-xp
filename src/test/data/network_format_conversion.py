import xml.etree.ElementTree as ET
from xml.dom import minidom
from datetime import datetime
import os

def convert_to_osm(input_file, output_file):
    # Parse MATSim XML
    tree = ET.parse(input_file)
    root = tree.getroot()

    # Create OSM root
    osm = ET.Element('osm', version='0.6')
    
    # Convert nodes
    node_id = 1
    for node in root.findall('.//node'):
        osm_node = ET.SubElement(osm, 'node', {
            'id': node.get('id'),
            'lat': str(float(node.get('y'))), # Convert to rough lat
            'lon': str(float(node.get('x'))),  # Convert to rough lon
            'version': '1',
            'timestamp': datetime.now().isoformat()
        })

    # Convert links to ways
    way_id = 1
    for link in root.findall('.//link'):
        way = ET.SubElement(osm, 'way', {
            'id': link.get('id'),
            'version': '1',
            'timestamp': datetime.now().isoformat()
        })
        
        # Add nodes refs
        ET.SubElement(way, 'nd', ref=link.get('from'))
        ET.SubElement(way, 'nd', ref=link.get('to'))
        
        # Add tags
        tags = {
            'highway': 'unclassified',
            'oneway': 'yes',
            'lanes': link.get('permlanes'),
            'maxspeed': str(round(float(link.get('freespeed')) * 3.6)), # Convert m/s to km/h
            'capacity': link.get('capacity'),
            'modes': link.get('modes')
        }
        
        for k, v in tags.items():
            tag = ET.SubElement(way, 'tag', k=k, v=v)
            
        way_id += 1

    # # Write to file
    # tree = ET.ElementTree(osm)
    # tree.write(output_file, encoding='utf-8', xml_declaration=True)

    # Write to file
    # Convert ElementTree to string
    xmlstr = ET.tostring(osm, encoding='utf-8')
    # Create pretty formatted XML 
    dom = minidom.parseString(xmlstr)
    pretty_xml = dom.toprettyxml(indent="  ")
    
    # Write formatted XML to file
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(pretty_xml)

if __name__ == "__main__":
    print(os.listdir('./'))
    convert_to_osm(r'test_equil_raw.xml', r'test_equil_V2.osm')