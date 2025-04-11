# Standard library imports
import os
import logging
# Data handling and analysis
import numpy as np
import pandas as pd
import geopandas as gpd
from sklearn.preprocessing import StandardScaler

# Statistical modeling
import statsmodels.api as sm
from statsmodels.stats.outliers_influence import variance_inflation_factor
from mgwr.gwr import GWR
from mgwr.sel_bw import Sel_BW

# Spatial analysis
import libpysal as lps
import pysal.explore as ps_explore
import pysal.model as ps_model

# Visualization
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.colors import ListedColormap
import contextily as ctx
from splot.esda import lisa_cluster, plot_moran_simulation
import mapclassify


''' Utility functions '''

def get_intervals(mc, fmt="{:.2f}"):
    lowest = mc.y.min()
    if hasattr(mc, "lowest") and mc.lowest is not None:
        lowest = mc.lowest
    lower_open = False
    if lowest > mc.bins[0]:
        lowest = -np.inf
        lower_open = True
    edges = [lowest]
    edges.extend(mc.bins)
    edges = [fmt.format(edge) for edge in edges]
    max_width = max([len(edge) for edge in edges])
    return edges, max_width, lower_open

def classify_data_with_zero_div_pos(data: np.ndarray, 
                                k=3,
                                fmt="{:.2f}", 
                                # deviation=0.01, 
                                method='natural_breaks'):

    if method == 'natural_breaks':
        mc = mapclassify.NaturalBreaks(data, k=k)
    elif method == 'quantiles':
        mc = mapclassify.Quantiles(data, k=k)
    elif method == 'equal_interval':
        mc = mapclassify.EqualInterval(data, k=k)
    elif method == 'fisher_jenks':
        mc = mapclassify.FisherJenks(data, k=k)
    elif method == 'head_tail_breaks':
        mc = mapclassify.HeadTailBreaks(data)
    elif method == 'percentiles':
        mc = mapclassify.Percentiles(data)
    elif method == 'pretty':
        mc = mapclassify.PrettyBreaks(data, k=k)
    elif method == 'std_mean':
        mc = mapclassify.StdMean(data)
    elif method == 'jenks_caspall':
        mc = mapclassify.JenksCaspall(data, k=k)
    elif method == 'max_p_':
        mc = mapclassify.MaxP(data, k=k)
    elif method == 'maximum_breaks':
        mc = mapclassify.MaximumBreaks(data, k=k)
    else:
        raise ValueError(f"Unknown classification method: {method}")
    
    intervals: list = get_intervals(mc, fmt=fmt)[0]

    return intervals

def classify_data_with_zero_div_neg(data: np.ndarray, 
                                k=3,
                                fmt="{:.2f}", 
                                # deviation=0.01, 
                                run_time=0,
                                method='natural_breaks'):

    min_data = np.min(data)

    if method == 'natural_breaks':
        mc = mapclassify.NaturalBreaks(data, k=k)
    elif method == 'quantiles':
        mc = mapclassify.Quantiles(data, k=k)
    elif method == 'equal_interval':
        mc = mapclassify.EqualInterval(data, k=k)
    elif method == 'fisher_jenks':
        mc = mapclassify.FisherJenks(data, k=k)
    elif method == 'head_tail_breaks':
        mc = mapclassify.HeadTailBreaks(data)
    elif method == 'percentiles':
        mc = mapclassify.Percentiles(data)
    elif method == 'pretty':
        mc = mapclassify.PrettyBreaks(data, k=k)
    elif method == 'std_mean':
        mc = mapclassify.StdMean(data)
    elif method == 'jenks_caspall':
        mc = mapclassify.JenksCaspall(data, k=k)
    elif method == 'max_p_':
        mc = mapclassify.MaxP(data, k=k)
    elif method == 'maximum_breaks':
        mc = mapclassify.MaximumBreaks(data, k=k)
    else:
        raise ValueError(f"Unknown classification method: {method}")
    
    intervals: list = get_intervals(mc, fmt=fmt)[0]

    if round(min_data, 2) == round(float(intervals[0]),2) \
    and round(min_data, 2) == round(float(intervals[1]),2) \
    and run_time == 0:  # If the min_data is included
        # Re-run the classification
        intervals: list =  classify_data_with_zero_div_neg(data, k=k+1, 
                                            fmt=fmt, run_time=1,
                                            method=method)
        intervals = intervals[1:]

        return intervals

    # Change the last one in the intervals to 0
    intervals[-1] = '0'


    return intervals

def classify_data_with_zero_div(data: np.ndarray,
                                sub_k=3,
                                fmt="{:.2f}", 
                                run_time=0,
                                deviation=0.01,
                                method='natural_breaks',
                                color_list=None):
    neg_intervals = classify_data_with_zero_div_neg(data[data<=0], 
                                                    k=sub_k, 
                                                    fmt=fmt, 
                                                    run_time=run_time, 
                                                    method=method)
    pos_intervals = classify_data_with_zero_div_pos(data[data>0],
                                                    k=sub_k, 
                                                    fmt=fmt, 
                                                    method=method)
    neg_intervals = [round(float(interval),2) for interval in neg_intervals]
    pos_intervals = [round(float(interval),2) for interval in pos_intervals]

    neg_intervals[0] = round(neg_intervals[0] - deviation, 2)
    pos_intervals[-1] = round(pos_intervals[-1] + deviation, 2)

    all_intervals = neg_intervals + pos_intervals[1:]

    print(f'All intervals: {all_intervals}')

    # Create a list ;of tuples with the intervals and the corresponding colors
    bins = list(zip(all_intervals[:-1], all_intervals[1:]))
    bins = []
    for i, (lb, ub) in enumerate(zip(all_intervals[:-1], all_intervals[1:])):
        if i == 0:
            bins.append((lb, ub))
        else:
            bins.append((round(lb+deviation, 2), ub))
    
    color_dict = {}
    for i in range(len(bins)):
        color_dict[bins[i]] = color_list[i]
    
    return all_intervals, bins, color_dict
        
def format_coef_sci_math(c, use_brackets=True, sig_digits=2):
    """
    将数值格式化为两位有效数字，并以 `×10⁻⁴` 或 `(...10⁻⁴)` 的形式输出
    - use_brackets: 是否使用 (10⁻⁴)，否则用 ×10⁻⁴
    """
    if pd.isna(c):
        return 'NaN'

    if c == 0:
        return '0'

    abs_c = abs(c)
    if abs_c < 1e-2 or abs_c >= 1e4:
        s = f"{c:.{sig_digits}e}"
        base, exp = s.split('e')
        base = base.rstrip('0').rstrip('.')  # 去除多余小数
        exp = int(exp)

        # 构造上标
        superscript_map = str.maketrans("-0123456789", "⁻⁰¹²³⁴⁵⁶⁷⁸⁹")
        exp_sup = str(exp).translate(superscript_map)

        if use_brackets:
            return f"{base} (10{exp_sup})"
        else:
            return f"{base}×10{exp_sup}"
    else:
        return f"{round(c, 2)}"  # 正常数字，两位有效数字
    
import matplotlib.patches as mpatches
def add_scalebar(ax,lon0,lat0,length,size=0.45, style=1, unit_deviations=100, scalebar_fontsize=10):
    '''
    ax: 坐标轴
    lon0: 经度
    lat0: 纬度
    length: 长度
    size: 控制粗细和距离的
    '''
    if style == 1:
        ax.hlines(y=lat0,  xmin = lon0, xmax = lon0+length, colors="black", ls="-", lw=1, label='%d m' % (length))
        ax.vlines(x = lon0, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=1)
        ax.vlines(x = lon0+length/2, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=1)
        ax.vlines(x = lon0+length, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=1)
        ax.text(lon0+length,lat0+size+20,str(int(length/1000)),horizontalalignment = 'center', fontdict={'size': scalebar_fontsize})
        ax.text(lon0+length/2,lat0+size+20,str(float(length/2/1000)),horizontalalignment = 'center', fontdict={'size': scalebar_fontsize})
        ax.text(lon0,lat0+size+20,'0',horizontalalignment = 'center', fontdict={'size': scalebar_fontsize})
        ax.text(lon0+length+unit_deviations,lat0+size+20,'Kilometers',horizontalalignment = 'left', fontdict={'size': scalebar_fontsize})
    
    elif style == 2:
    # print(help(ax.vlines))
        ax.hlines(y=lat0,  xmin = lon0, xmax = lon0+length/111, colors="black", ls="-", lw=2, label='%d km' % (length))
        ax.vlines(x = lon0, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=2)
        ax.vlines(x = lon0+length/111, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=2)
        # ax.text(lon0+length/2/111,lat0+size,'500 km',horizontalalignment = 'center')
        ax.text(lon0+length/2/111,lat0+size,'%d' % (length/2),horizontalalignment = 'center')
        ax.text(lon0,lat0+size,'0',horizontalalignment = 'center')
        ax.text(lon0+length/111/2*3,lat0+size,'km',horizontalalignment = 'center')

    elif style == 3:
       
        plt.hlines(y=lat0,  xmin = lon0, xmax = lon0+length/111, colors="black", ls="-", lw=1, label='%d km' % (length))
        plt.vlines(x = lon0, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=1)
        plt.vlines(x = lon0+length/111, ymin = lat0-size, ymax = lat0+size, colors="black", ls="-", lw=1)
        plt.text(lon0+length/111,lat0+size,'%d km' % (length),horizontalalignment = 'center')
        plt.text(lon0,lat0+size,'0',horizontalalignment = 'center')
    
    else:
        raise ValueError('style must be 1 or 2 or 3')

def add_north(ax, labelsize=18, loc_x=0.95, loc_y=0.99, width=0.06, height=0.09, pad=0.14):
    """
    画一个比例尺带'N'文字注释
    主要参数如下
    :param ax: 要画的坐标区域 Axes实例 plt.gca()获取即可
    :param labelsize: 显示'N'文字的大小
    :param loc_x: 以文字下部为中心的占整个ax横向比例
    :param loc_y: 以文字下部为中心的占整个ax纵向比例
    :param width: 指南针占ax比例宽度
    :param height: 指南针占ax比例高度
    :param pad: 文字符号占ax比例间隙
    :return: None
    """
    minx, maxx = ax.get_xlim()
    miny, maxy = ax.get_ylim()
    ylen = maxy - miny
    xlen = maxx - minx
    left = [minx + xlen*(loc_x - width*.5), miny + ylen*(loc_y - pad)]
    right = [minx + xlen*(loc_x + width*.5), miny + ylen*(loc_y - pad)]
    top = [minx + xlen*loc_x, miny + ylen*(loc_y - pad + height)]
    center = [minx + xlen*loc_x, left[1] + (top[1] - left[1])*.4]
    triangle = mpatches.Polygon([left, top, right, center], color='k')
    ax.text(s='N',
            x=minx + xlen*loc_x,
            y=miny + ylen*(loc_y - pad + height),
            fontsize=labelsize,
            horizontalalignment='center',
            verticalalignment='bottom')
    ax.add_patch(triangle)

def classify_data_with_k_colors(data: np.ndarray, color_list: list, n_digit=2, deviation=0.01, min_threshold=0,
                                run_time=0,
                                k=None,
                                method='natural_breaks'):
    if k is None:
        k = len(color_list)
    print(f'k={k}')

    if method == 'natural_breaks':
        bins = mapclassify.NaturalBreaks(data, k=k).bins.tolist()
    elif method == 'quantiles':
        bins = mapclassify.Quantiles(data, k=k).bins.tolist()
    elif method == 'equal_interval':
        bins = mapclassify.EqualInterval(data, k=k).bins.tolist()
    elif method == 'fisher_jenks':
        bins = mapclassify.FisherJenks(data, k=k).bins.tolist()
    elif method == 'head_tail_breaks':
        bins = mapclassify.HeadTailBreaks(data).bins.tolist()
    elif method == 'percentiles':
        bins = mapclassify.Percentiles(data).bins.tolist()
    elif method == 'pretty':
        bins = mapclassify.PrettyBreaks(data, k=k).bins.tolist()
    elif method == 'std_mean':
        bins = mapclassify.StdMean(data).bins.tolist()
    elif method == 'jenks_caspall':
        bins = mapclassify.JenksCaspall(data, k=k).bins.tolist()
    elif method == 'max_p_':
        bins = mapclassify.MaxP(data, k=k).bins.tolist()
    elif method == 'maximum_breaks':
        bins = mapclassify.MaximumBreaks(data, k=k).bins.tolist()
    else:
        raise ValueError(f"Unknown classification method: {method}")
    
    ''' If the minimum value of data is in the bins, 
    we need to classify the data again with k+1 colors.
    This is because the minimum value is not included in the bins,
    and the classification may not be accurate.
    '''
    if min(data) in bins and run_time == 0:  # If this is the first run and the minimum value is in the bins
        return classify_data_with_k_colors(data, color_list,
                                           n_digit, deviation, min_threshold, k=len(color_list)+1, method=method, run_time=1)
    elif min(data) not in bins and run_time == 0:  # If this is the first run and the minimum value is not in the bins
        bins = [min(data)] + bins
    else:
        pass
    bins = [round(b-deviation, n_digit) if b != bins[-1] else round(b+deviation, n_digit) for b in bins]
    if min_threshold == 0:
        bins[0] = 0
    bin_color_dict = {}
    for i, (start, end) in enumerate(zip(bins[:-1], bins[1:])):
        bin_color_dict[(start, end)] = color_list[i]

    print(f'bin_color_dict: {bin_color_dict}')
    print(f'bins: {bins}')
    
    return bin_color_dict, bins

def custom_natural_breaks_with_zero_split(data, k=6, color_list=None, subgroup_max_k=4,
                                        method='natural_breaks', round_digits=2):
    """
    Split data into k classes with a forced split at 0
    
    Parameters:
    -----------
    data : array-like
        Input data
    k : int, default=6
        Number of classes
    color_list : list, optional
        List of colors for visualization
    subgroup_max_k : int, default=4
        Maximum number of classes for negative or positive subgroups
    method : str, default='natural_breaks'
        Classification method
    round_digits : int, default=2
        Number of decimal places to round
        
    Returns:
    --------
    breaks : list
        List of classification boundaries
    """
    data = np.asarray(data)
    data = data[~np.isnan(data)]
    
    # Split data into negative and positive parts
    neg_data = data[data < 0]
    pos_data = data[data >= 0]
    
    # Determine number of classes for each part
    if len(neg_data) == 0:
        n_neg_classes = 0
        n_pos_classes = k
    elif len(pos_data) == 0:
        n_neg_classes = k
        n_pos_classes = 0
    else:
        neg_ratio = len(neg_data) / len(data)
        n_neg_classes = max(1, min(subgroup_max_k, round(k * neg_ratio)))
        n_pos_classes = k - n_neg_classes
    
    breaks = []
    
    # Handle negative values
    if n_neg_classes > 0 and len(neg_data) > 0:
        neg_classifier = classify_data_with_k_colors(neg_data, k=n_neg_classes)
        neg_breaks = neg_classifier.bins.tolist()
        breaks.extend([float(np.min(data))] + neg_breaks)
    
    # Add 0 as split point
    if n_neg_classes > 0 and n_pos_classes > 0:
        breaks.append(0.0)
    
    # Handle positive values
    if n_pos_classes > 0 and len(pos_data) > 0:
        pos_classifier = mapclassify.NaturalBreaks(pos_data, k=n_pos_classes)
        pos_breaks = pos_classifier.bins.tolist()
        if n_neg_classes > 0:
            breaks.extend([b for b in pos_breaks if b > 0])
        else:
            breaks.extend([0.0] + pos_breaks)
    
    # Ensure max value is included
    if breaks[-1] < float(np.max(data)):
        breaks[-1] = float(np.max(data))
    
    # Remove duplicates and sort
    breaks = sorted(list(set(breaks)))
    
    # Round values
    breaks = [round(x, round_digits) for x in breaks]
    
    return breaks

''' Function to plot spatial patterns of emissions '''
def plot_spatial_pattern_emission(geofiles: dict[str, gpd.GeoDataFrame],
                                  scneario_kw: str,  # Specify the scenario keyword
                                  anls_col: str,
                                  color_list: list[str],
                                  **kwargs):
    ''' Break the data into n classes '''
    # Aggregate the data
    data = pd.concat([geofiles[scenario][anls_col] for scenario in geofiles.keys()])
    data = data.to_numpy()
    # Get the classification bins
    bin_color_dict, bins = classify_data_with_k_colors(data, color_list, method=kwargs.get('method', 'natural_breaks'))

    ''' Plot '''
    fig, ax = plt.subplots(figsize=kwargs.get('fig_size', (5, 5)), dpi=kwargs.get('dpi', 350))
    
    geofile = geofiles[scneario_kw]

    # Assign the colors to the data
    new_geo_file = geofile.copy(deep=True)
    new_geo_file['group'] = pd.cut(new_geo_file[anls_col], bins, labels=bin_color_dict.keys())
    new_geo_file['color'] = new_geo_file['group'].map(bin_color_dict)
    
    # Plot the data
    new_geo_file.plot(ax=ax, color=new_geo_file['color'])

    # Make the plot look nice
    ax.set_aspect('auto')

    ''' add base map '''
    ctx.add_basemap(ax, 
                    crs=geofile.crs, 
                    source=ctx.providers.CartoDB.Positron,
                    alpha=0.8,
                    attribution=False)
    
    ''' add scale bar and north arrow '''
    add_north(ax, width=kwargs.get('north_width',0.045), height=kwargs.get('north_height', 0.09), pad=kwargs.get('north_pad', 0.14), labelsize=kwargs.get('N_size', 8))
    add_scalebar(ax, 171300, 172850, 1000, size=kwargs.get('scalebar_size', 20), style=1, unit_deviations=kwargs.get('scalebar_dev', 80),
                 scalebar_fontsize=kwargs.get('scalebar_fontsize', 8))

    ''' Add background color '''
    bounds = ax.get_position().bounds 
    rect = plt.Rectangle((0, 0), 1, 1,
                      facecolor=kwargs.get('bg_color', 'white'),
                      alpha=kwargs.get('bg_alpha', 0.1),  
                      transform=ax.transAxes, 
                      zorder=1000)  # ensure the background color is at the upper layer
    ax.add_patch(rect)

    ''' Delete the axis '''
    plt.axis('off')

    if kwargs.get('print_bins'):
        print(bin_color_dict)
    plt.tight_layout()
    plt.show()

    if kwargs.get('output_dir') and kwargs.get('output_filename'):
        os.makedirs(kwargs.get('output_dir'), exist_ok=True)
        fig.savefig(kwargs.get('output_dir') + kwargs.get('output_filename'), 
                    bbox_inches='tight', 
                    pad_inches=0, 
                    dpi=350
                    )


    ''' Moran I '''
def cal_global_moran(geofile: gpd.GeoDataFrame,
                    anls_col: str,  # The column to be analyzed (e.g., 'EPI_mean', 'weighted_AQI_mean')
                    ):
    ''' calculate the spatial weights '''
    spatial_weights = lps.weights.Queen.from_dataframe(geofile)
    spatial_weights.transform = 'r'
    ''' calculate the global moran I '''
    moran_I = ps_explore.esda.Moran(geofile[anls_col], spatial_weights)
    ''' Print the Moran I summary '''
    moran_summary = {}
    moran_summary['I'] = moran_I.I
    moran_summary['P-norm'] = moran_I.p_norm
    moran_summary['P-rand'] = moran_I.p_rand
    moran_summary['P-sim'] = moran_I.p_sim
    moran_summary['Z-norm'] = moran_I.z_norm
    moran_summary['Z-rand'] = moran_I.z_rand
    moran_summary['Z-sim'] = moran_I.z_sim 

    print(f'Global Moran I summary: {moran_summary}')
    return moran_I, moran_summary
    
def plot_global_moran(moran_I: ps_explore.esda.Moran,
                    moran_summary: dict,
                    bg_color: str = None,
                    bg_alpha: float = 1,
                    output_dir: str = None,
                    output_filename: str = None,
                    **kwargs):
                    
    ''' Plot the Moran I '''
    fig, ax = plt.subplots(figsize=kwargs.get('figsize', (4,3)), dpi=350)

    plot_moran_simulation(moran_I, aspect_equal=False, ax=ax, 
                        color=bg_color, alpha=bg_alpha,
                        edgecolor=bg_color, linewidth=1, linestyle=':',
                        fitline_kwds={'color': 'orange', 'linewidth': 1})
                        

    ''' Delete the title '''
    ax.set_title('')

    ''' Get the vlines and change the style of vlines '''
    for collection in ax.collections:
        if hasattr(collection, 'get_segments'):
            if collection.get_segments()[0][0][0] == moran_I.EI:
                EI_line = collection
                EI_x = EI_line.get_segments()[0][0][0]
                EI_y = EI_line.get_segments()[0][1][1]
            elif collection.get_segments()[0][0][0] == moran_I.I:
                I_line = collection
                I_x = I_line.get_segments()[0][0][0]
                I_y = I_line.get_segments()[0][1][1]
            else:
                continue
    EI_line.set_color('m')
    EI_line.set_linewidth(3)
    I_line.set_color('Salmon')
    I_line.set_linewidth(3)

    # Add text on the EI and I lines
    ax.text(EI_x, EI_y+0.5, round(moran_I.EI, 3), fontsize=9, color='m', ha='center', va='bottom')
    ax.text(I_x, I_y+0.5, round(moran_I.I, 3), fontsize=9, color='Salmon', ha='center', va='bottom')
    

    ''' Set the shade color '''
    # if bg_color:
        # ax.add_patch(plt.Rectangle((0, 0), 1, 1,
        #              facecolor=bg_color,
        #              alpha=bg_alpha,
        #              transform=ax.transAxes,
        #              zorder=-1))


    ''' Set x/y labels '''
    if kwargs.get('is_labels', True):
        ax.set_xlabel('Moran I', fontdict={'size': 12, 'weight': 'bold', 'family': 'Arial'})
        ax.set_ylabel('Density', fontdict={'size': 12, 'weight': 'bold', 'family': 'Arial'})
    else:
        ax.set_xlabel('')
        ax.set_ylabel('')

    
    ''' Set the x/y ticks fontsize '''
    ax.tick_params(axis='x', labelsize=10)
    ax.tick_params(axis='y', labelsize=10)

    ''' Set the x/y axis lims'''
    ax.set_xlim(kwargs.get('xlim', (-0.2, 0.5)))
    ax.set_ylim(kwargs.get('ylim', (0, 15)))

    ''' Set the x/y axis spines color '''

    plt.tight_layout()
    plt.show()
    if output_dir and output_filename:
        os.makedirs(output_dir, exist_ok=True)
        fig.savefig(output_dir + output_filename,
                    bbox_inches='tight',
                    pad_inches=0,
                    transparent=True,
                    dpi=350
                    )

''' Plot lisa cluster '''
def cal_and_plot_lisa_clusters(geofile: gpd.GeoDataFrame,
                               anls_col: str,  # The column to be analyzed (e.g., 'EPI_mean', 'weighted_AQI_mean')
                               **kwargs,
                               ):
    ''' calculate the spatial weights '''
    spatial_weights = lps.weights.Queen.from_dataframe(geofile)
    spatial_weights.transform = 'r'
    ''' calculate the local moran I '''
    lisa = ps_explore.esda.Moran_Local(geofile[anls_col], spatial_weights)
    
    ''' Plot the official lisa cluster for reference '''
    lisa_cluster(lisa, geofile, p=0.05, figsize=(3, 3))

    ''' Get the cluster labels '''
    cluster_labels = lisa.get_cluster_labels()
    new_geofile = geofile.copy(deep=True)
    new_geofile['cluster'] = cluster_labels

    ''' plot the lisa cluster '''
    fig, ax = plt.subplots(figsize=kwargs.get('fig_size', (4, 4)), dpi=350)
    

    # Make the customized color map from the color_list
    if 'color_dict' in kwargs:
        color_dict = kwargs['color_dict']
        # Delete the color without corresponding cluster
        unique_labels = new_geofile['cluster'].unique().tolist()
        color_dict = {k: color_dict[k] for k in unique_labels}

        # 排序后生成分类值列表
        clusters = list(color_dict.keys())
        clusters_sorted = sorted(clusters)
        # 按照 cluster 顺序创建颜色列表
        color_list = [color_dict[c] for c in clusters_sorted]
        # 创建 colormap 和 norm
        cmap = ListedColormap(color_list)
        # norm = BoundaryNorm(boundaries=[c - 0.5 for c in clusters_sorted] + [clusters_sorted[-1] + 0.5], ncolors=len(clusters_sorted))

    else:
        cmap = plt.cm.tab20

    new_geofile.plot(column='cluster', cmap=cmap, 
                #  norm=norm,
                    ax=ax, legend=kwargs.get('is_legend', False), 
                    alpha=0.8
                    )
    # Make the plot look nice
    ax.set_aspect('auto')

    ''' add base map '''
    ctx.add_basemap(ax, 
                    crs=geofile.crs, 
                    source=ctx.providers.CartoDB.Positron,
                    alpha=0.8,
                    attribution=False)
    
    ''' add scale bar and north arrow '''
    add_north(ax, width=0.045, height=0.09, pad=0.14, labelsize=kwargs.get('N_size', 8))
    add_scalebar(ax, 171300, 172850, 1000, size=kwargs.get('scalebar_size', 20), style=1, unit_deviations=80,
                 scalebar_fontsize=kwargs.get('scalebar_fontsize', 8))

    ''' Add background color '''
    bounds = ax.get_position().bounds 
    rect = plt.Rectangle((0, 0), 1, 1,
                      facecolor=kwargs.get('bg_color', 'white'),
                      alpha=kwargs.get('bg_alpha', 0.1),  
                      transform=ax.transAxes, 
                      zorder=1000)  # ensure the background color is at the upper layer
    ax.add_patch(rect)

    ''' Delete the axis '''
    plt.axis('off')

    plt.tight_layout()
    plt.show()

    # print("Figure size (inches):", fig.get_size_inches())
    # print("Figure size (pixels):", fig.get_size_inches() * fig.dpi)
    if kwargs.get('output_dir') and kwargs.get('output_filename'):
        os.makedirs(kwargs.get('output_dir'), exist_ok=True)
        fig.savefig(kwargs.get('output_dir') + kwargs.get('output_filename'), 
                    bbox_inches='tight', 
                    pad_inches=0, 
                    dpi=350
                    )
    
''' OLS and GWR utils '''
def OLS_reg(geofile: gpd.GeoDataFrame,
            dep_vars: str,  # The dependent variables
            ind_vars: list,  # The independent variables 
            is_strandardize: bool = False,  # Whether to standardize the independent variables
            is_vif: bool = True,  # Whether to calculate the VIF
            ):
    ''' Fill the nan values '''
    new_geofile= geofile.fillna(0)

    ''' Get the dependent variables '''
    Y = new_geofile[dep_vars].values.reshape((-1, 1))
    ''' Get the independent variables '''
    Xs = new_geofile[ind_vars].values
    ''' Standardize the independent variables '''
    if is_strandardize:
        Xs = StandardScaler().fit_transform(Xs)
    ''' Add the constant '''
    Xs = sm.add_constant(Xs)
    ''' OLS regression '''
    ols_model = sm.OLS(Y, Xs).fit()
    ''' Calculate the VIF '''
    if is_vif:
        vif = pd.DataFrame()
        vif["variables"] = ['Intercept'] + ind_vars
        vif["VIF"] = [variance_inflation_factor(Xs, i) for i in range(Xs.shape[1])]
        print(vif)
    ''' Print the summary of the OLS regression '''
    print(ols_model.summary())
    return ols_model

def GWR_reg(geofile: gpd.GeoDataFrame,
            dep_vars: str,  # The dependent variables
            ind_vars: list,  # The independent variables
            is_strandardize: bool = True,  # Whether to standardize the independent variables
            ):
    ''' Fill the nan values '''
    new_geofile= geofile.fillna(0)

    ''' Get the unit coordinates '''
    unit_coords = np.column_stack((new_geofile['geometry'].centroid.x, new_geofile['geometry'].centroid.y))

    ''' Get the dependent variables '''
    Y = new_geofile[dep_vars].values.reshape((-1, 1))

    ''' Get the independent variables '''
    Xs = new_geofile[ind_vars].values
    
    if is_strandardize:
        Xs = StandardScaler().fit_transform(Xs)
    ''' Add the constant '''
    Xs = sm.add_constant(Xs)
    
    ''' GWR regression '''
    gwr_selector = Sel_BW(unit_coords, Y, Xs)
    gwr_bw = gwr_selector.search()
    print(f"GWR最佳带宽：{gwr_bw}")

    gwr_model = GWR(unit_coords, Y, Xs, bw=gwr_bw)
    gwr_results = gwr_model.fit()
    print(f'GWR Results Summary: {gwr_results.summary()}')
    print(f'GWR R2: {gwr_results.R2}')
    print(f'GWR AICc: {gwr_results.aicc}')

    for i, name in enumerate(ind_vars):
        new_geofile[f'gwr_{name}'] = gwr_results.params[:, i+1]
    return gwr_results, new_geofile

def plot_scenarios_ols_coef_and_std(scenarios_olsModels: dict,  # {scenario: ols_model},
                                    scenario_colors: dict,  # {scenario: color}
                                    param_names: list,  # The parameter names, the order should be consistent with the model
                                    **kwargs
                                    ):
    fig, ax = plt.subplots(figsize=kwargs.get('fig_size', (5, 4)), 
                           dpi=500)
    
    if kwargs.get('y_lim'):
        ax.set_ylim(kwargs.get('y_lim'))
    
    if kwargs.get('x_lim'):
        ax.set_xlim(kwargs.get('x_lim'))

    for scenario, ols_model in scenarios_olsModels.items():
        coef = ols_model.params[1:]
        std = ols_model.bse[1:]
        # Plot the bar chart with error bars, each scenario has a different color and loc in x-axis
        ax.bar(np.arange(len(param_names)) + (list(scenarios_olsModels.keys()).index(scenario) - 1) * 0.2, coef, 
               yerr=std, 
               error_kw={
                'capsize': 2,        # 横线长度，控制"I"的短横
                'capthick': 0.5,     # 横线粗细
                'elinewidth': 0.8,   # 竖线粗细
                'ecolor': 'black'    # 误差棒颜色
                 },
               color=scenario_colors[scenario], 
               width=0.2, 
               label=f'{scenario} (R²: {round(ols_model.rsquared, 2)})',
               alpha=0.8)
        
        # Add the text annotation
        for i, (c, s) in enumerate(zip(coef, std)):
            ''' For very small coefficients, we use 10**n to represent the coefficient in the text annotation '''
            
            new_c = format_coef_sci_math(c)
                
            if c > 0:
                ax.text(i + (list(scenarios_olsModels.keys()).index(scenario)-1) * 0.2, 
                        c + s + 0.6, 
                        f'{new_c}', 
                        ha='center', 
                        va='bottom', 
                        color=scenario_colors[scenario],
                        rotation=-45,
                        fontweight='bold',
                        fontsize=kwargs.get('coef_fontsize', 10))
            else:
                ax.text(i + (list(scenarios_olsModels.keys()).index(scenario)-1) * 0.2, 
                        c - s - 0.6, 
                        f'{new_c}', 
                        ha='center', 
                        va='top', 
                        color=scenario_colors[scenario],
                        rotation=-45,
                        fontweight='bold',
                        fontsize=kwargs.get('coef_fontsize', 10))
            # ax.text(i + list(scenarios_olsModels.keys()).index(scenario) * 0.2, c - s - 0.05, f'{s:.2f}', ha='center', va='top', fontsize=8)    
            
    ax.axhline(0, color='black', linestyle='--', linewidth=0.5)

    if kwargs.get('is_y_label', True):
        ax.set_ylabel('Coefficient', fontdict={'size': 12, 'weight': 'bold', 'family': 'Arial'})
    else:
        ax.set_ylabel('')
    
    if kwargs.get('is_x_label', False) == False:
        ax.set_xlabel('')
    else:
        ax.set_xlabel('Parameters', fontdict={'size': 12, 'weight': 'bold', 'family': 'Arial'})

    if kwargs.get('is_xticks', True):
        ax.set_xticks(np.arange(len(param_names)))
        ax.set_xticklabels(param_names, rotation=-45, ha='left', fontsize=10)
    else:
        ax.set_xticklabels([])
    
    ''' delete the top and right spines '''
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    
    
    ''' Change the legend frame as dashed '''
    if kwargs.get('is_legend_frame'):
        ax.legend(loc='upper right', prop={'size': 8}, frameon=True)
        legend = ax.get_legend()
        legend.get_frame().set_linestyle('--')

    ax.legend(loc='upper right', prop={'size': kwargs.get('legend_fontsize', 10)}, frameon=False)
        
    plt.tight_layout()
    plt.show()

    if kwargs.get('output_dir') and kwargs.get('output_filename'):
        os.makedirs(kwargs.get('output_dir'), exist_ok=True)
        fig.savefig(kwargs.get('output_dir') + kwargs.get('output_filename'), 
               
                    bbox_inches='tight', 
                    pad_inches=0, 
                    transparent=True,
                    )

def plot_scenarios_ols_coef_and_std_horizontal(scenarios_olsModels: dict,  # {scenario: ols_model},
                                               scenario_colors: dict,  # {scenario: color}
                                               param_names: list,  # The parameter names, the order should be consistent with the model
                                               **kwargs
                                               ):
    # 调整 figsize 为竖版布局，比如 (宽度, 高度) 设为 (4, 5)
    fig, ax = plt.subplots(figsize=kwargs.get('fig_size', (4, 5)), dpi=500)
    
    # 如果有设置 x_lim 和 y_lim，则应用（注意：x_lim 现在表示系数轴）
    if kwargs.get('x_lim'):
        ax.set_xlim(kwargs.get('x_lim'))
    if kwargs.get('y_lim'):
        ax.set_ylim(kwargs.get('y_lim'))

    # 对于每个情景（scenario）绘制水平条形图（横向柱状图）
    for scenario, ols_model in scenarios_olsModels.items():
        # 注意这里剔除常数项（假设常数在第一个位置）
        coef = ols_model.params[1:]
        std = ols_model.bse[1:]
        # 计算每个参数对应的 y 坐标，每个情景略有偏移以便区分
        y_positions = np.arange(len(param_names)) + (list(scenarios_olsModels.keys()).index(scenario) - 1) * 0.2
        
        # 画横向条形图，误差棒需要用 xerr 参数
        ax.barh(y_positions, coef, 
                xerr=std, 
                error_kw={
                    'capsize': 2,        # 误差棒端部横线长度
                    'capthick': 0.5,     # 误差棒横线粗细
                    'elinewidth': 0.8,   # 误差棒主线粗细
                    'ecolor': 'black'    # 误差棒颜色
                 },
                color=scenario_colors[scenario], 
                height=0.2, 
                label=f'{scenario} (R²: {round(ols_model.rsquared, 2)})',
                alpha=0.8)
        
        # 添加数值注释
        for i, (c, s) in enumerate(zip(coef, std)):
            # 使用科学计数法格式化系数的文本，函数 format_coef_sci_math 需要你自己定义
            new_c = format_coef_sci_math(c)
            y_val = i + (list(scenarios_olsModels.keys()).index(scenario) - 1) * 0.2
            if c > 0:
                # 当系数为正时，文本位于条形图右侧
                ax.text(c + s + 0.5, y_val, f'{new_c}', 
                        ha='left', va='center', 
                        color=scenario_colors[scenario],
                        # rotation=-45,
                        fontweight='bold',
                        fontsize=kwargs.get('coef_fontsize', 10))
            else:
                # 当系数为负时，文本位于条形图左侧
                ax.text(c - s - 0.5, y_val, f'{new_c}', 
                        ha='right', va='center', 
                        color=scenario_colors[scenario],
                        # rotation=-45,
                        fontweight='bold',
                        fontsize=kwargs.get('coef_fontsize', 10))
    
    # 添加 x=0 的参考线，注意这里用 ax.axvline 画垂直线
    ax.axvline(0, color='black', linestyle='--', linewidth=0.5)

    # 设置 x 轴和 y 轴的标签：x轴显示系数，y轴显示参数名称
    if kwargs.get('is_x_label', True):
        ax.set_xlabel('Coefficient', fontdict={'size': 12, 'weight': 'bold', 'family': 'Arial'})
    else:
        ax.set_xlabel('')
    
    if kwargs.get('is_y_label'):
        ax.set_ylabel('Parameters', fontdict={'size': 12, 'weight': 'bold', 'family': 'Arial'})
    else:
        ax.set_ylabel('')

    # 设置 y 轴刻度为参数名称
    if kwargs.get('is_yticks', True):
        ax.set_yticks(np.arange(len(param_names)))
    else:
        ax.set_yticklabels([])
    
    if kwargs.get('is_yticklabels'):
        ax.set_yticklabels(param_names, fontsize=10)
    else:
        ax.set_yticklabels([])

    # 可选：如果需要设置 x 轴刻度，可以按需调整
    if kwargs.get('is_xticks', True):
        ax.set_xticks(kwargs.get('x_ticks', ax.get_xticks()))
    
    # 删除顶部和右侧的边框
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    
    # 图例设置：根据是否需要图例边框调整
    if kwargs.get('is_legend_frame'):
        ax.legend(loc='best', prop={'size': 8}, frameon=True)
        legend = ax.get_legend()
        legend.get_frame().set_linestyle('--')
    else:
        ax.legend(loc='upper right', prop={'size': kwargs.get('legend_fontsize', 10)}, frameon=False)
    
    if kwargs.get('is_legend') == False:
        ax.legend().set_visible(False)

    ''' 将tick的刻度线加厚 '''
    ax.tick_params(axis='both', which='major', width=1.5, length=10)

    plt.tight_layout()
    plt.show()

    # 如果传入输出路径和文件名，则保存图像
    if kwargs.get('output_dir') and kwargs.get('output_filename'):
        os.makedirs(kwargs.get('output_dir'), exist_ok=True)
        fig.savefig(kwargs.get('output_dir') + kwargs.get('output_filename'), 
                    bbox_inches='tight', 
                    pad_inches=0, 
                    transparent=True)

''' GWR utils '''
def filter_incorrect_coef(gwr_gdf: gpd.GeoDataFrame, 
                        col_name: str, 
                        ):
    data = gwr_gdf[col_name].to_numpy() # 替换成你的数据

    # Calculate Q1, Q3 and IQR
    q1 = np.percentile(data, 25)
    q3 = np.percentile(data, 75)
    iqr = q3 - q1

    # Set lower and upper bounds for outliers
    lower_bound = q1 - 100 * iqr
    upper_bound = q3 + 100 * iqr

    # 去除异常值
    filtered_data = data[(data >= lower_bound) & (data <= upper_bound)]
    mean_filtered = np.mean(filtered_data)

    new_gwr_gdf = gwr_gdf.copy(deep=True)
    # 将异常值替换为均值
    new_gwr_gdf.loc[(data < lower_bound) | (data > upper_bound), col_name] = mean_filtered
    return new_gwr_gdf, mean_filtered
    
def plot_gwr_map(gwr_gdfs: dict, # {scenario: gwr_result_gdf},):
                scenario_kw: str,
                metric_name: str,  # The metric (var) name to be plotted
                color_list = ['#1D425D', '#2A8CA9', '#9ACDD1', '#F99D74', '#DB0801', '#8C2633'],
                **kwargs):

    
    if color_list is None:
        color_list = ['#1D425D', '#2A8CA9', '#9ACDD1', '#F99D74', '#DB0801', '#8C2633']

    ''' Break the data into n classes '''
    # Aggregate the data
    data = pd.concat([gwr_gdfs[scenario][metric_name] for scenario in gwr_gdfs.keys()])
    data = data.to_numpy()

    bins, group_bins, bin_color_dict,  = classify_data_with_zero_div(data, sub_k=int(len(color_list)/2),
                                                        method=kwargs.get('method', 'natural_breaks'), 
                                                        color_list=color_list, 
                                                        )
    print(f'The classes of color-bins: {bin_color_dict} and bins {bins}')

    ''' Plot '''
    fig, ax = plt.subplots(figsize=kwargs.get('fig_size', (5, 5)), dpi=kwargs.get('dpi', 350))

    ''' Set x/y lim '''
    if kwargs.get('x_lim'):
        ax.set_xlim(kwargs.get('x_lim'))
    if kwargs.get('y_lim'):
        ax.set_ylim(kwargs.get('y_lim'))

    geofile = gwr_gdfs[scenario_kw]
    new_geofile = geofile.copy(deep=True)  

    new_geofile['group'] = pd.cut(new_geofile[metric_name], bins, labels=bin_color_dict.keys())
    new_geofile['color'] = new_geofile['group'].map(bin_color_dict)

    print("Unique color indices:", new_geofile['color'].unique())
    print("Unique groups:", new_geofile['group'].unique())
    cluster_labels = [bin_color_dict.get(group_) for group_ in new_geofile['group'].unique()]
    print("Unique cluster labels:", cluster_labels)
    '''Plot the data'''
    new_geofile.plot(ax=ax, color=new_geofile['color'], alpha=kwargs.get('data_alpha', 0.8), 
                    #  label=cluster_labels if kwargs.get('is_legend') else None,
                    #  zorder=-5
                     )
    
    ''' add base map '''
    ctx.add_basemap(ax, 
                    crs=geofile.crs, 
                    source=ctx.providers.CartoDB.Positron,
                    alpha=0.8,
                    attribution=False)

    add_north(ax, width=kwargs.get('north_width',0.045), 
              height=kwargs.get('north_height', 0.09), 
              pad=kwargs.get('north_pad', 0.14), 
              labelsize=kwargs.get('N_size', 8),
            #   zorder=100
              )
    add_scalebar(ax, 171300, 172850, 1000, size=kwargs.get('scalebar_size', 20), style=1, unit_deviations=kwargs.get('scalebar_dev', 80),
                 scalebar_fontsize=kwargs.get('scalebar_fontsize', 8), 
                #  zorder=100
                 )

    ''' Add background color '''
    bounds = ax.get_position().bounds 
    rect = plt.Rectangle((0, 0), 1, 1,
                      facecolor=kwargs.get('bg_color', 'white'),
                      alpha=kwargs.get('bg_alpha', 0.1),  
                      transform=ax.transAxes, 
                      zorder=1000)  # ensure the background color is at the upper layer
    ax.add_patch(rect)

    if kwargs.get('print_bins'):
        print(bin_color_dict)

    ''' Hide the axis '''
    if kwargs.get('hide_axis'):
        ax.axis('off')

    # Make the plot look nice
    ax.set_aspect('auto')

    if kwargs.get('is_legend', False):
        plt.legend(loc='upper right', prop={'size': 8}, frameon=True)

    plt.tight_layout()
    if kwargs.get('is_show'):
        plt.show()  

    if kwargs.get('output_dir') and kwargs.get('output_filename'):
        os.makedirs(kwargs.get('output_dir'), exist_ok=True)
        fig.savefig(kwargs.get('output_dir') + kwargs.get('output_filename'), 
                    bbox_inches='tight', 
                    pad_inches=0, 
                    dpi=350)

if __name__ == "__main__":
    ''' Set the logger '''
    logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S',
                    handlers=[logging.FileHandler("log.txt"), logging.StreamHandler()]
                    )

    ''' Set the working directory '''
    # Set the vars to be analyzed
    ''' NOTE: The dependent variable, which could be:
        'EPI_mean', 'PM_total_mean', 'PM2_5_total_mean', 'NOx_mean', 'CO_mean', 'SO2_mean', 'NO2_mean',
    '''
    dependent_var = "PM_total_mean"  
    independent_vars = [
        "intersection_count",
        "network_density",
        "mean_distance",
        "depot_count",
        "degree_centrality",
        "closeness",
        #   'eigenvector',
        "betweeness",
    ]

    ''' Read aggregated emission geo-files '''

    scenario_types = ['Basic','Van','CB']
    geofile_root = r'../../../../data/clean/freightEmissions/shp/stats/'

    scenario_geofiles = {}
    for scenario in scenario_types: 
        scenario_geofiles[scenario] = gpd.read_file(geofile_root + scenario + '_stats_h3Res10V2.geojson')
    
    print(f'columns in the agg-emission geo-file: {(scenario_geofiles["Basic"]).columns}')

    ''' Check the unit of the dependent variable '''
    logging.warning(f'The dependent variable is {dependent_var}')
    logging.info(f'the mean of {dependent_var} in the Basic scenario: {scenario_geofiles["Basic"][dependent_var].mean()}')
    is_scale = input("Do you want to scale the data? (y/n)")
    if is_scale.lower() == 'y':
        scale_level = int(input("Please input the scale level (e.g., 1, 2, 3):"))
        scale_level = 10 ** scale_level
        for scenario in scenario_types:
            scenario_geofiles[scenario][dependent_var] = scenario_geofiles[scenario][dependent_var] * scale_level
        logging.warning(f'The data is scaled by {scale_level}')
        logging.info(f'The mean of {dependent_var} in the Basic scenario after scaling: {scenario_geofiles["Basic"][dependent_var].mean()}')

    ''' Plot spatial patterns of emissions'''

    c_list = ['#DADAE8', '#BCBDDA', '#9D9AC4', '#736CAC', '#4D2987']
    emission_spatial_pattern_root = r'../../../../figures/freightEmissions/spEmissions//'
    # recommended method: max_p_, quantiles, pretty
    method = 'max_p_'
    for scen_kw, bg_c, bg_a in zip(scenario_types, ['grey', 'blue', 'lime'], [0.1, 0.04, 0.005]):
        plot_spatial_pattern_emission(geofiles=scenario_geofiles, 
                                    anls_col=dependent_var,
                                    scneario_kw=scen_kw,
                                    color_list=['#DADAE8', '#BCBDDA', '#9D9AC4', '#736CAC', '#4D2987'],
                                    method=method,
                                    fig_size=(1.9, 1.7),
                                    print_bins=True,
                                    north_pad=0.2,
                                    scalebar_size=50,
                                    N_size=8,
                                    bg_color=bg_c,
                                    bg_alpha=bg_a,
                                    output_dir=emission_spatial_pattern_root + f'{dependent_var}/{method}/',
                                    output_filename=scen_kw + f'.png'
                                    )
    # Only plot the Basic and Van scenarios (as some pollutants are not available for CB) 
    for scen_kw, bg_c, bg_a in zip(['Basic', 'Van'], ['grey', 'blue'], [0.1, 0.04]):
        plot_spatial_pattern_emission(geofiles={scen_kw: scenario_geofiles[scen_kw]}, 
                                    anls_col=dependent_var,
                                    scneario_kw=scen_kw,
                                    color_list=['#DADAE8', '#BCBDDA', '#9D9AC4', '#736CAC', '#4D2987'],
                                    method='natural_breaks',
                                    fig_size=(4.5, 4),
                                    print_bins=True,
                                    N_size=8,
                                    bg_color=bg_c,
                                    bg_alpha=bg_a,
                                    output_dir=emission_spatial_pattern_root,
                                    output_filename=scen_kw + f'_{dependent_var}.png'
                                    )
    
    ''' Plot spatial patterns of VKT '''
    c_list = ['#DADAE8', '#BCBDDA', '#9D9AC4', '#736CAC', '#4D2987']
    vkt_unit_root = r'../../../../data/clean/freightEmissions/shp/statsWithVKT/'
    vkt_figure_output_root = r'../../../../figures/freightEmissions/spVkt/'

    vkt_unit_geofiles = {scen_kw: gpd.read_file(vkt_unit_root + scen_kw + '_vkt_unit.geojson')
                        for scen_kw in scenario_types}
    
    # recommended method: max_p_, (kinda time consuming); quantiles, (very quick); pretty
    for scen_kw, bg_c, bg_a in zip(scenario_types, ['grey', 'blue', 'lime'], [0.1, 0.04, 0.005]):
        plot_spatial_pattern_emission(geofiles=vkt_unit_geofiles, 
                                    anls_col='mean_vkt',
                                    scneario_kw=scen_kw,
                                    color_list=c_list,
                                    method='natural_breaks',
                                    fig_size=(2.5, 2.3),
                                    print_bins=True,
                                    N_size=8,
                                    bg_color=bg_c,
                                    bg_alpha=bg_a,
                                    # output_dir=vkt_figure_output_root,
                                    # output_filename=scen_kw + '_mean_vkt_nb.png'
                                    )
    
    ''' Moran I '''
    moran_fig_output_dir = r'../../../../figures/freightEmissions/moranI/'
    global_moran_I_scenario_color_alpha = {'basic': ('slategray', 0.5), 
                                       'van': ('steelblue', 0.5), 
                                       'cb': ('g', 0.2)}
    lisa_colors = {'Insignificant': '#FCF7F1', 
                    'Low-Low': '#2B6AA8',
                    'Low-High': '#C0D9EB',
                    'High-Low': '#FFC3AC',
                    'High-High': '#D26C69'}
    lisa_bg_color_alpha = {'basic': ('gray', 0.1), 
                            'van': ('blue', 0.03), 
                            'cb': ('g', 0.02)}
    
    for scenario, geo_file in scenario_geofiles.items():
        print(f'Processing the scenario: {scenario}')
        _global_color, _global_alpha = global_moran_I_scenario_color_alpha[scenario.lower()]
        _lisa_color, _lisa_alpha = lisa_bg_color_alpha[scenario.lower()]

        ''' Calculate the global Moran I '''
        moran_I, moran_summary = cal_global_moran(geo_file, dependent_var)

        ''' Plot the global Moran I '''
        plot_global_moran(moran_I, moran_summary, 
                        bg_color=_global_color, bg_alpha=_global_alpha, 
                        figsize=(4.2,2.8),
                        is_labels=False,
                        output_dir=moran_fig_output_dir, 
                        output_filename=f'{scenario}_{dependent_var}_global_moranI.png'
                        )
        
        ''' Calculate and plot the lisa cluster '''
        cal_and_plot_lisa_clusters(geo_file, dependent_var, 
                                fig_size=(4, 3.5),
                                bg_color=_lisa_color, bg_alpha=_lisa_alpha,
                                output_dir=moran_fig_output_dir, 
                                output_filename=f'{scenario}_{dependent_var}_lisa_cluster.png',
                                N_size=8, scalebar_size=20, scalebar_fontsize=8,
                                is_legend=False,
                                color_dict=lisa_colors
                                )
        
    ''' OLS '''
    scenario_ols_models = {}
    for scenario, geofile in scenario_geofiles.items():
        scenario_ols_models[scenario] = OLS_reg(geofile, dependent_var, independent_vars, is_strandardize=True)
    
    # Vertical
    plot_scenarios_ols_coef_and_std(scenario_ols_models,
                                {'Basic': 'gray', 'Van': 'steelblue', 'CB': '#A5D6A7'},
                                ['IntCnt', 'NetDen', 'DepotDist', 'DepotCnt', 'DegCent', 'CloCent', 'BetCent'],
                                fig_size=(13, 3.6),
                                y_lim=(-6,6),
                                # output_dir=r'../../../../figures/freightEmissions/gwr/',
                                # output_filename='OLS_coef_std.png',
                                coef_fontsize=10,
                                legend_fontsize=10,
                                # is_xticks=False
                                )
    
    # Horizontal
    plot_scenarios_ols_coef_and_std_horizontal(scenario_ols_models,
                                {'Basic': 'gray', 'Van': 'steelblue', 'CB': '#A5D6A7'},
                                ['IntCnt', 'NetDen', 'DepotDist', 'DepotCnt', 'DegCent', 'CloCent', 'BetCent'],
                                fig_size=(4, 11),
                                x_lim=(-5.9,4),
                                output_dir=r'../../../../figures/freightEmissions/gwr/',
                                output_filename=f'{dependent_var}_OLS_coef_std_horizontal.png',
                                coef_fontsize=10,
                                legend_fontsize=10,
                                is_xticks=False,
                                is_legend=False
                                )
    
    ''' GWR '''
    scenario_gwr_results = {}
    scenario_gwr_gdfs = {}
    for scenario, geofile in scenario_geofiles.items():
        scenario_gwr_results[scenario], scenario_gwr_gdfs[scenario] = GWR_reg(geofile, dependent_var, independent_vars)

    for scen_kw, gwr_result in scenario_gwr_results.items():
        print(f'{scen_kw} GWR R²: {gwr_result.R2}')
        print(f'{scen_kw} GWR AICc: {gwr_result.aicc}')
    
    metric_name_list = [col for col in scenario_gwr_gdfs['Basic'].columns if 'gwr_' in col]
    print(f'The GWR metric names: {metric_name_list}')

    # Correct the incorrect coef
    fil_scenario_gwr_gdfs = {}
    for scenario, gdf in scenario_gwr_gdfs.items():
        if fil_scenario_gwr_gdfs.get(scenario) is None:
            fil_scenario_gwr_gdfs[scenario] = gdf.copy(deep=True)
        for metric_name in metric_name_list:
            fil_scenario_gwr_gdfs[scenario] = filter_incorrect_coef(fil_scenario_gwr_gdfs[scenario], metric_name)[0]
            print(f'Filter the incorrect coef for {scenario} {metric_name}') 

    # Plot gwr maps        
    for scen_kw, bg_c, bg_a in zip(['Basic', 'Van', 'CB'], 
                               ['grey', 'blue', 'g'], 
                               [0.1, 0.05, 0.04]):
        for metric_n in metric_name_list:
            print(f'{scen_kw} {metric_n}')
            plot_gwr_map(gwr_gdfs=fil_scenario_gwr_gdfs,
                        scenario_kw=scen_kw,
                            metric_name=metric_n,
                            color_list=['#1D425D', '#2A8CA9', '#9ACDD1', '#F99D74', '#DB0801', '#8C2633'],
                            hide_axis=True,
                            fig_size=(1.9, 1.7),
                            dpi=350,
                            bg_color=bg_c,
                            bg_alpha=bg_a,
                            N_size=8,
                            north_pad=0.16,
                            scalebar_size=80,
                            scalebar_fontsize=9,
                            method='natural_breaks',
                            output_dir=r'../../../../figures/freightEmissions/gwr//' + f'{dependent_var}/naturalBreaks/'+scen_kw.lower()+'/',
                            output_filename=f'{scen_kw}_{metric_n}.png',
                            is_show=False,
                            is_legend=False,
                            )
