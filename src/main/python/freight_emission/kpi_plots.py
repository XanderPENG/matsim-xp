import numpy as np
import matplotlib.pyplot as plt 
import matplotlib.colors as mcolors
import os
import scipy.stats as stats
# Some utils modules
import pollutants
import freight_emissions_anls as fea

# Change the default font family as Arial
plt.rcParams["font.sans-serif"] = "Arial"

''' Plot utils '''

def darken_color(color, factor=0.7):

    rgb = np.array(mcolors.to_rgb(color))
    dark_rgb = rgb * factor
    return dark_rgb


def plot_multigroup_stat_comparison(
    result_summary_dict,  # {scen_kw: result_summary}
    config_dict,  # {scen_kw: (color, alpha, linestyle)}
    xlabel,
    figure_folder,
    filename=None,
    only_fitting_curve=True,
    figure_size=(10, 6),
    is_fitting=True,
    n_bins=50,
    **kwargs
):
    fig, ax = plt.subplots(figsize=figure_size, dpi=350)
    min_val = min([result_summary.min() for result_summary in result_summary_dict.values()])
    max_val = max([result_summary.max() for result_summary in result_summary_dict.values()])

    bins = np.linspace(min_val, max_val, n_bins)

    # Plot histograms
    for scen_kw, result_summary in result_summary_dict.items():
        color, alpha, linestyle = config_dict[scen_kw]
        if only_fitting_curve is False:
            # Plot histogram
            ax.hist(
                result_summary, bins=bins, color=color, alpha=alpha,
                label=scen_kw
            )

        if is_fitting:
            # bin宽度
            bin_width = bins[1] - bins[0]
            xmin, xmax = result_summary.min(), result_summary.max()
            print(f'scenario: {scen_kw}; min: {xmin}; max: {xmax}')
            xmin, xmax = (xmin- (max_val - min_val) * 0.05, xmax + (max_val - min_val) * 0.05)
            x = np.linspace(xmin, xmax, 500)

            # 对 result_summary 拟合正态分布，并调整曲线高度
            mu_car, std_car = stats.norm.fit(result_summary)
            p_car = stats.norm.pdf(x, mu_car, std_car) * len(result_summary) * bin_width
            ax.plot(x, p_car, color=darken_color(color), 
                    linewidth=2, linestyle=linestyle,
                    label=f"{scen_kw} Fitting")
            
    # Hide the right and top spines
    plt.gca().spines["right"].set_visible(False)
    plt.gca().spines["top"].set_visible(False)

    plt.xlabel(xlabel, fontsize=kwargs.get('label_size', 12), fontweight="bold")
    plt.ylabel("Density", fontsize=kwargs.get('label_size', 12), fontweight="bold")

    plt.xticks(fontsize=kwargs.get('tick_size', 12))
    plt.yticks(fontsize=kwargs.get('tick_size', 12))
    plt.tight_layout()
    plt.savefig(figure_folder + filename, dpi=350,
                bbox_inches='tight', 
                pad_inches=0,
                transparent=True)


def plot_stat_comparison(
    bike_summary,
    car_summary,
    no_policy_summary,
    xlabel,
    figure_folder,
    filename=None,
    colors=["darkgrey", "steelblue", "#A5D6A7"],
    alphas=[0.8, 0.6, 0.8],
    figure_size=(10, 6),
    is_fitting=False,
    n_bins=60,
    **kwargs
):
    fig, ax = plt.subplots(figsize=figure_size, dpi=300)
    min_val = min(bike_summary.min(), car_summary.min(), no_policy_summary.min())
    max_val = max(bike_summary.max(), car_summary.max(), no_policy_summary.max())

    bins = np.linspace(min_val, max_val, n_bins)
    
    # Plot histograms

    # 绘制非归一化直方图（显示频数）
    ax.hist(no_policy_summary, bins=bins, color=colors[0], 
                                alpha=alphas[0], label="Van")
    ax.hist(car_summary, bins=bins, color=colors[1], alpha=alphas[1], 
            label="Van-Circulation")
    ax.hist(bike_summary, bins=bins, color=colors[2], alpha=alphas[2], 
            label="Cargo Bike-Circulation")
    if is_fitting:
        # bin宽度
        bin_width = bins[1] - bins[0]
        xmin, xmax = plt.xlim()
        x = np.linspace(xmin, xmax, 1000)

        # 对 no_policy_summary 拟合正态分布，并调整曲线高度
        mu_van, std_van = stats.norm.fit(no_policy_summary)
        p_van = stats.norm.pdf(x, mu_van, std_van) * len(no_policy_summary) * bin_width
        ax.plot(x, p_van, color=darken_color(colors[0], 0.5), linewidth=2, linestyle=':', label="Van Fitting")

        # 对 car_summary 拟合正态分布，并调整曲线高度
        mu_car, std_car = stats.norm.fit(car_summary)
        p_car = stats.norm.pdf(x, mu_car, std_car) * len(car_summary) * bin_width
        ax.plot(x, p_car, color=darken_color(colors[1]), linewidth=2, linestyle=':', label="Van-Circulation Fitting")

        # 对 bike_summary 拟合正态分布，并调整曲线高度
        mu_bike, std_bike = stats.norm.fit(bike_summary)
        p_bike = stats.norm.pdf(x, mu_bike, std_bike) * len(bike_summary) * bin_width
        ax.plot(x, p_bike, color=darken_color(colors[2], 0.9), linewidth=2, linestyle=':', label="Cargo Bike-Circulation Fitting")

    # Hide the right and top spines
    plt.gca().spines["right"].set_visible(False)
    plt.gca().spines["top"].set_visible(False)

    plt.xlabel(xlabel, fontsize=kwargs.get('label_size', 12), fontweight="bold")
    plt.ylabel("Density", fontsize=kwargs.get('label_size', 12), fontweight="bold")

    plt.xticks(fontsize=kwargs.get('tick_size', 12))
    plt.yticks(fontsize=kwargs.get('tick_size', 12))
    plt.tight_layout()
    plt.savefig(figure_folder + filename, dpi=350,
                # bbox_inches='tight', 
                # pad_inches=0,
                transparent=True)
    # plt.show()


def plot_stat_comparison_two_groups(
    car_summary,
    no_policy_summary,
    xlabel,
    figure_folder,
    filename=None,
    colors=["darkgrey", "steelblue"],
    alphas=[0.8, 0.6],
    figure_size=(10, 6),
    is_fitting=True,
    n_bins=50,
):
    fig, ax = plt.subplots(figsize=figure_size, dpi=350)
    min_val = min(car_summary.min(), no_policy_summary.min())
    max_val = max(car_summary.max(), no_policy_summary.max())

    bins = np.linspace(min_val, max_val, n_bins)

    plt.hist(
        no_policy_summary, bins=bins, color=colors[0], alpha=alphas[0], label="Van"
    )
    plt.hist(
        car_summary,
        bins=bins,
        color=colors[1],
        alpha=alphas[1],
        label="Van-Circulation",
    )

    if is_fitting:
        # bin宽度
        bin_width = bins[1] - bins[0]
        xmin, xmax = plt.xlim()
        x = np.linspace(xmin, xmax, 1000)

        # 对 no_policy_summary 拟合正态分布，并调整曲线高度
        mu_van, std_van = stats.norm.fit(no_policy_summary)
        p_van = stats.norm.pdf(x, mu_van, std_van) * len(no_policy_summary) * bin_width
        ax.plot(x, p_van, color=darken_color(colors[0], 0.5), linewidth=2, linestyle=':', label="Van Fitting")

        # 对 car_summary 拟合正态分布，并调整曲线高度
        mu_car, std_car = stats.norm.fit(car_summary)
        p_car = stats.norm.pdf(x, mu_car, std_car) * len(car_summary) * bin_width
        ax.plot(x, p_car, color=darken_color(colors[1]), linewidth=2, linestyle=':', label="Van-Circulation Fitting")

    # Hide the right and top spines
    plt.gca().spines["right"].set_visible(False)
    plt.gca().spines["top"].set_visible(False)

    plt.xlabel(xlabel, fontsize=10, fontweight="bold")
    plt.ylabel("Density", fontsize=10, fontweight="bold")
    plt.xticks(fontsize=10)
    plt.yticks(fontsize=10)
    plt.tight_layout()
    plt.savefig(figure_folder + filename,
                bbox_inches='tight', 
                pad_inches=0,
                transparent=True)
    # plt.show()

def plot_stat_one_group(
    result_summary,
    xlabel,
    figure_folder,
    filename=None,
    color="steelblue",
    alpha=0.6,
    figure_size=(10, 6),
    is_fitting=True,
    n_bins=50,
    **kwargs
):
    fig, ax = plt.subplots(figsize=figure_size, dpi=350)
    if kwargs.get('x_lim') is not None:
        plt.xlim(kwargs.get('x_lim'))
    if kwargs.get('y_lim') is not None:
        plt.ylim(kwargs.get('y_lim'))
    min_val = result_summary.min()
    max_val = result_summary.max()

    bins = np.linspace(min_val, max_val, n_bins)

    plt.hist(
        result_summary, bins=bins, color=color, alpha=alpha,
        # label="Van"
    )

    if is_fitting:
        # bin宽度
        bin_width = bins[1] - bins[0]
        xmin, xmax = plt.xlim()
        x = np.linspace(xmin, xmax, 1000)

        # 对 result_summary 拟合正态分布，并调整曲线高度
        mu_car, std_car = stats.norm.fit(result_summary)
        p_car = stats.norm.pdf(x, mu_car, std_car) * len(result_summary) * bin_width
        ax.plot(x, p_car, color=darken_color(color), linewidth=2, linestyle=':', 
                # label="Van-Circulation Fitting"
                )

    # Hide the right and top spines
    plt.gca().spines["right"].set_visible(False)
    plt.gca().spines["top"].set_visible(False)
    
    plt.xlabel(xlabel, fontsize=10, fontweight="bold")
    plt.ylabel("Density", fontsize=10, fontweight="bold")
    if kwargs.get('hide_labels', True):
        plt.xlabel('')
        plt.ylabel('')
    plt.xticks(fontsize=10)
    plt.yticks(fontsize=10)
    plt.tight_layout()
    plt.savefig(figure_folder + filename,
                bbox_inches='tight', 
                pad_inches=0,
                transparent=True)
    # plt.show()

if __name__ == '__main__':
    ''' Some settings '''
    iter_list = list(range(300, 400))
    scenario_kw_list = [
    'basic',
    'van', 
    'cb'
    ]
    sa_kw_color_dict = {
        # 'BasicSA2t': ('#B7B7B7', 0.8, 'dotted'),
        # 'BasicSA4t': ('#B7B7B7', 0.8, 'dashdot'),
        # 'VanSA2t': ('#6184a1', 0.6, 'dotted'),
        # 'VanSA4t': ('#6184a1', 0.8, 'dashdot'),
        # 'CBSA80kg': ('#e6e5b8', 0.95, 'dotted'),
        # 'CBSA100kg': ('#c7cfb7', 0.9, 'dotted'),
        # 'CBSA150kg': ('#9dad7f', 0.85, 'dashdot'),
        # 'CBSA200kg': ('#819f85', 0.95, 'dashdot'),
    }
    sa_scenario_kw_list = list(sa_kw_color_dict.keys())
    sa_colors = [tup[0] for tup in list(sa_kw_color_dict.values())]
    sa_alphas = [tup[1] for tup in list(sa_kw_color_dict.values())]
    sa_linestyles = [tup[2] for tup in list(sa_kw_color_dict.values())]

    figure_folder = r'../../../../figures/freightEmissions/KPIs/'
    sa_figure_folder = r'../../../../figures/freightEmissions/SA/'
    os.makedirs(figure_folder, exist_ok=True)
    os.makedirs(sa_figure_folder, exist_ok=True)

    plt.rcParams["font.sans-serif"] = "Arial"

    # Load all the scenario stats
    all_scenario_stats = fea.load_all_scenario_stats(scenario_kw_list=scenario_kw_list+sa_scenario_kw_list,
                                                     iter_list=iter_list)
    all_scenario_emissions = fea.load_all_scenario_emission_stats(scenario_kw_list=scenario_kw_list+sa_scenario_kw_list, 
                                                                  iter_list=iter_list)
    print(f'Category of emissions: {list(all_scenario_emissions.values())[0].keys()}')

    def plot_main_plots(figure_folder,):
        ''' VKT '''
        metric = 'vkt'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='vkt.png',
            xlabel='VKT (km)',
            # n_bins=50,
            is_fitting=True,
            figure_size=(6.2, 2.2)

        )

        # Only compare van and no policy
        metric = 'vkt'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='vkt_van_no_policy.png',
            xlabel='VKT (km)',
            n_bins=50,
            is_fitting=True,
            figure_size=(2.5, 1.2)
        )

        # only cb 
        metric = 'vkt'
        plot_stat_one_group(
            result_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='vkt_cb.png',
            xlabel='VKT (km)',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(2.5, 1.2),
            is_fitting=True,
            n_bins=50,
        )

        ''' Transit VKT '''
        metric = 'transit_vkt'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='transit_vkt.png',
            xlabel='Transit VKT (km)',
            figure_size=(3.9, 2.6)
        )

        # Only compare van and no policy
        metric = 'transit_vkt'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='transit_vkt_van_no_policy.png',
            xlabel='Transit VKT (km)',
        )

        ''' Transit Time '''
        metric = 'total_transit_time'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='transit_time.png',
            xlabel='Transit Time (min)',
            figure_size=(6.2, 2.2),
            is_fitting=True,
        )

        # Only compare van and no policy
        metric = 'total_transit_time'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='transit_time_van_no_policy.png',
            xlabel='Transit Time (min)',
            n_bins=50,
            is_fitting=True,
            figure_size=(2.5, 1.2)
        )

        # only cb
        metric = 'total_transit_time'
        plot_stat_one_group(
            result_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='transit_time_cb.png',
            xlabel='Transit Time (min)',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(2.5, 1.2),
            is_fitting=True,
            n_bins=50,
        )

        ''' Ton-km traveled '''
        metric = 'ton_km_traveled'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='ton_km_traveled.png',
            xlabel='Ton-km traveled (ton·km)',
            figure_size=(6.2, 2.2),
            is_fitting=True,

        )

        # Only compare van and no policy
        metric = 'ton_km_traveled'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='ton_km_traveled_van_no_policy.png',
            xlabel='Ton-km traveled (ton-km)',
            n_bins=50,
            is_fitting=True,
            figure_size=(2.5, 1.2)
        )

        # only cb
        metric = 'ton_km_traveled'
        plot_stat_one_group(
            result_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='ton_km_traveled_cb.png',
            xlabel='Ton-km traveled (ton·km)',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(2.5, 1.2),
            is_fitting=True,
            n_bins=50,
        )

        ''' Transit Time per Ton '''
        metric = 'transit_vkt_per_ton'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='transit_vkt_per_ton.png',
            xlabel='Transit VKT per ton (Km/ton)',
        )

        # Only compare van and no policy
        metric = 'transit_vkt_per_ton'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='transit_vkt_per_ton_van_no_policy.png',
            xlabel='Transit VKT per ton (Km/ton)',
        )

        ''' Transit Time per Ton '''
        metric = 'transit_time_per_ton'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='transit_time_per_ton.png',
            xlabel='Transit Time per ton (min/ton)',
        )

        # Only compare van and no policy
        metric = 'transit_time_per_ton'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='transit_time_per_ton_van_no_policy.png',
            xlabel='Transit Time per ton (min/ton)',
        )

        ''' Number of vehicles '''
        metric = 'number_of_vehicle'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            bike_summary=np.array(all_scenario_stats['cb'][metric]),
            figure_folder=figure_folder,
            filename='number_of_vehicle.png',
            xlabel='Number of vehicles',
        )

        # Only compare van and no policy
        metric = 'number_of_vehicle'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
            car_summary=np.array(all_scenario_stats['van'][metric]),
            figure_folder=figure_folder,
            filename='number_of_vehicle_van_no_policy.png',
            xlabel='Number of vehicles',
        )

        ''' Pollutants-CO2e'''
        metric = pollutants.CO2e
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            bike_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='CO2e.png',
            xlabel='WTW CO2-eq emissions (g)',
            figure_size=(13, 3.6),
            is_fitting=True,
        )

        # Only compare van and no policy
        metric = pollutants.CO2e
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            figure_folder=figure_folder,
            filename='CO2e_van_no_policy.png',
            xlabel='WTW CO2-eq emissions (g)',
            n_bins=50,
            is_fitting=True,
            figure_size=(2.6, 1.2)
        )

        # only cb
        metric = pollutants.CO2e
        plot_stat_one_group(
            result_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='CO2e_cb.png',
            xlabel='WTW CO2-eq emissions (g)',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(2.6, 1.2),
            is_fitting=True,
            n_bins=50,
        )

        ''' Pollutants-air quality '''
        metric = 'air_quality_pollutants'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            bike_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='air_quality.png',
            xlabel='Air quality emissions (g)',
        )

        # Only compare van and no policy
        metric = 'air_quality_pollutants'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            figure_folder=figure_folder,
            filename='air_quality_van_no_policy.png',
            xlabel='Air quality emissions (g)',
        )

        ''' EPI '''
        metric = 'EPI'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            bike_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='EPI.png',
            xlabel='EPI',
            is_fitting=True,
            figure_size=(5.5, 3),
        )

        # Only compare van and no policy
        metric = 'EPI'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            figure_folder=figure_folder,
            filename='EPI_van_no_policy.png',
            xlabel='EPI',
            n_bins=50,
            is_fitting=True,
            figure_size=(2.4, 1.5)
        )

        # only cb
        metric = 'EPI'
        plot_stat_one_group(
            result_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='EPI_cb.png',
            xlabel='EPI',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(2.4, 1.5),
            is_fitting=True,
            n_bins=50,
        )

        ''' Weighted AQI '''
        metric = 'weighted_AQI'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            bike_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='weighted_AQI.png',
            xlabel='Weighted AQI',
        )

        # Only compare van and no policy
        metric = 'weighted_AQI'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            figure_folder=figure_folder,
            filename='weighted_AQI_van_no_policy.png',
            xlabel='Weighted AQI',
            n_bins=50,
            is_fitting=True,
            figure_size=(2.5, 1.5)
            )
        
        # only cb
        metric = 'weighted_AQI'
        plot_stat_one_group(
            result_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='weighted_AQI_cb.png',
            xlabel='Weighted AQI',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(2.5, 1.5),
            is_fitting=True,
            n_bins=50,
        )

        ''' PM_total'''
        metric = 'PM_total'
        plot_stat_comparison(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            bike_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='PM_total.png',
            xlabel='PM emissions (g)',
            is_fitting=True,
            figure_size=(5, 2),
        )
        # Only compare van and no policy
        metric = 'PM_total'
        plot_stat_comparison_two_groups(
            no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
            car_summary=np.array(all_scenario_emissions['van'][metric]),
            figure_folder=figure_folder,
            filename='PM_total_van_no_policy.png',
            xlabel='PM emissions (g)',
            n_bins=50,
            is_fitting=True,
            figure_size=(1.8, 1.1)
        )
        # only cb
        metric = 'PM_total'
        plot_stat_one_group(
            result_summary=np.array(all_scenario_emissions['cb'][metric]),
            figure_folder=figure_folder,
            filename='PM_total_cb.png',
            xlabel='PM emissions (g)',
            color="#A5D6A7",
            alpha=0.8,
            figure_size=(1.8, 1.1),
            is_fitting=True,
            n_bins=50,
        )

        ''' PM2_5_total'''

    
    def plot_sa_plots(sa_scenario_kw_list, sa_figure_folder, sa_colors, sa_alphas):
        ''' VKT '''
        metric = 'vkt' 
        for scen_kw, scen_color, scen_alpha in zip(sa_scenario_kw_list, sa_colors, sa_alphas):
            
            plot_stat_one_group(
                result_summary=np.array(all_scenario_stats[scen_kw][metric]),
                figure_folder=sa_figure_folder,
                filename=f'{scen_kw}_vkt.png',
                xlabel='VKT (km)',
                color=scen_color,
                alpha=scen_alpha,
                figure_size=(2.5, 1.2),
                is_fitting=True,
                n_bins=50,
                hide_labels=True,
                x_lim=(50, 300)
            )
        ''' Transit Time '''
        metric = 'total_transit_time'
        for scen_kw, scen_color, scen_alpha in zip(sa_scenario_kw_list, sa_colors, sa_alphas):

            plot_stat_one_group(
                result_summary=np.array(all_scenario_stats[scen_kw][metric]),
                figure_folder=sa_figure_folder,
                filename=f'{scen_kw}_transit_time.png',
                xlabel='Transit Time (min)',
                color=scen_color,
                alpha=scen_alpha,
                figure_size=(2.5, 1.2),
                is_fitting=True,
                n_bins=50,
                hide_labels=True,
                x_lim=(400, 1100)
            )
        ''' Ton-km traveled '''
        metric = 'ton_km_traveled'
        for scen_kw, scen_color, scen_alpha in zip(sa_scenario_kw_list, sa_colors, sa_alphas):
            plot_stat_one_group(
                result_summary=np.array(all_scenario_stats[scen_kw][metric]),
                figure_folder=sa_figure_folder,
                filename=f'{scen_kw}_ton_km_traveled.png',
                xlabel='Ton-km traveled (ton·km)',
                color=scen_color,
                alpha=scen_alpha,
                figure_size=(2.5, 1.2),
                is_fitting=True,
                n_bins=50,
                hide_labels=True,
                x_lim=(150, 850)
            )
        ''' EPI '''
        metric = 'EPI'
        for scen_kw, scen_color, scen_alpha in zip(sa_scenario_kw_list, sa_colors, sa_alphas):
            plot_stat_one_group(
                result_summary=np.array(all_scenario_emissions[scen_kw][metric]),
                figure_folder=sa_figure_folder,
                filename=f'{scen_kw}_EPI.png',
                xlabel='EPI',
                color=scen_color,
                alpha=scen_alpha,
                figure_size=(2.5, 1.2),
                is_fitting=True,
                n_bins=50,
                hide_labels=True,
                x_lim=(20, 1600)
            )

        ''' Weighted AQI '''
        metric = 'weighted_AQI'
        for scen_kw, scen_color, scen_alpha in zip(sa_scenario_kw_list, sa_colors, sa_alphas):
            plot_stat_one_group(
                result_summary=np.array(all_scenario_emissions[scen_kw][metric]),
                figure_folder=sa_figure_folder,
                filename=f'{scen_kw}_weighted_AQI.png',
                xlabel='Weighted AQI',
                color=scen_color,
                alpha=scen_alpha,
                figure_size=(2.5, 1.2),
                is_fitting=True,
                n_bins=50,
                hide_labels=True,
                # x_lim=(0, 100)
            )

        ''' Pollutants-CO2e '''
        metric = pollutants.CO2e
        for scen_kw, scen_color, scen_alpha in zip(sa_scenario_kw_list, sa_colors, sa_alphas):
            
            plot_stat_one_group(
                result_summary=np.array(all_scenario_emissions[scen_kw][metric]),
                figure_folder=sa_figure_folder,
                filename=f'{scen_kw}_CO2e.png',
                xlabel='WTW CO2-eq emissions (g)',
                color=scen_color,
                alpha=scen_alpha,
                figure_size=(2.5, 1.2),
                is_fitting=True,
                n_bins=50,
                hide_labels=True,
                x_lim=(750, 2700)
            )
    
    def plot_sa_figs_v2(sa_scenario_stats,
                        sa_scenario_emissions,
                        metric_name_table_dict,
                        sa_config_dict,
                        sa_figure_folder, 
                        only_fitting_curve=True):
        ''' 
        In this version, 
        there will only be one main figure for each metric (i.e., all the scenarios will be plotted in one figure).
        However, 2 sub-figures for each main figure will be created for Basic/Vans Scenario and CB scenario, respectively.
        Additionally, there is a switch of whether showing shades/bars, i.e., (only_fitting_curve=True).
        Thus, there will be 5 figures in total.
        '''
        
        if metric_name_table_dict is None:
            metric_name_table_dict = {
                'vkt': 'VKT (km)',
                'total_transit_time': 'Transit Time (min)',
                'ton_km_traveled': 'Ton-km traveled (ton·km)',
                'EPI': 'EPI',
                'weighted_AQI': 'Weighted AQI',
                pollutants.CO2e: 'WTW CO2-eq emissions (g)',
            }
        
        for metric in list(metric_name_table_dict.keys()):
            print(f'Plotting {metric}...')
            plot_multigroup_stat_comparison(
                result_summary_dict=
                    {scen_kw: np.array(stat[metric]) for scen_kw, stat in sa_scenario_emissions.items()} 
                    if metric in ['EPI', 'weighted_AQI', 'PM_total', 'PM2_5_total', 
                                  pollutants.CO, pollutants.NO2, pollutants.NOx, pollutants.SO2, pollutants.CO2e]
                    else {scen_kw: np.array(stat[metric]) for scen_kw, stat in sa_scenario_stats.items()},
                config_dict=sa_config_dict,
                xlabel=metric_name_table_dict[metric],
                figure_folder=sa_figure_folder,
                filename=f'sa_{metric}.png',
                only_fitting_curve=only_fitting_curve,
                figure_size=(6.2, 2.2),
                is_fitting=True,
                n_bins=50,
                tick_size=10,
                label_size=12,
            )
            
    plot_main_plots(figure_folder)
    # plot_sa_plots(sa_scenario_kw_list, sa_figure_folder, sa_colors, sa_alphas)

    # plot_sa_figs_v2(
    #     sa_scenario_stats=all_scenario_stats,
    #     sa_scenario_emissions=all_scenario_emissions,
    #     metric_name_table_dict={
    #             'vkt': 'VKT (km)',
    #             'total_transit_time': 'Transit Time (min)',
    #             'ton_km_traveled': 'Ton-km traveled (ton·km)',
    #             'PM_total': 'PM emissions (g)',
    #             'PM2_5_total': 'PM2.5 emissions (g)',
    #             pollutants.NOx: 'NOx emissions (g)',
    #             pollutants.NO2: 'NO2 emissions (g)',
    #             pollutants.SO2: 'SO2 emissions (g)',
    #             pollutants.CO: 'CO emissions (g)',
    #             pollutants.CO2e: 'WTW CO2-eq emissions (g)',
    #         },
    #     sa_config_dict=sa_kw_color_dict,
    #     sa_figure_folder=sa_figure_folder,
    #     only_fitting_curve=True
    # )
    print('Done!')
