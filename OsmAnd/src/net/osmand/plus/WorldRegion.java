package net.osmand.plus;

import android.content.res.Resources;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.download.DownloadActivityType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class WorldRegion {

	public static final String AFRICA_REGION_ID = "africa";
	public static final String ASIA_REGION_ID = "asia";
	public static final String AUSTRALIA_AND_OCEANIA_REGION_ID = "australia-oceania";
	public static final String CENTRAL_AMERICA_REGION_ID = "centralamerica";
	public static final String EUROPE_REGION_ID = "europe";
	public static final String NORTH_AMERICA_REGION_ID = "northamerica";
	public static final String RUSSIA_REGION_ID = "russia";
	public static final String SOUTH_AMERICA_REGION_ID = "southamerica";

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(WorldRegion.class);

	// Region data
	private String regionId;
	private String downloadsIdPrefix;
	private String name;

	private LatLon bboxTopLeft;
	private LatLon bboxBottomRight;

	private LinkedList<DownloadActivityType> resourceTypes;

	// Hierarchy
	private WorldRegion superregion;
	private LinkedList<WorldRegion> subregions;
	private LinkedList<WorldRegion> flattenedSubregions;

	private boolean purchased;
	private boolean isInPurchasedArea;

	public String getRegionId() {
		return regionId;
	}

	public String getDownloadsIdPrefix() {
		return downloadsIdPrefix;
	}

	public String getName() {
		return name;
	}

	public LatLon getBboxTopLeft() {
		return bboxTopLeft;
	}

	public LatLon getBboxBottomRight() {
		return bboxBottomRight;
	}

	public LinkedList<DownloadActivityType> getResourceTypes() {
		return resourceTypes;
	}

	public WorldRegion getSuperregion() {
		return superregion;
	}

	public LinkedList<WorldRegion> getSubregions() {
		return subregions;
	}

	public LinkedList<WorldRegion> getFlattenedSubregions() {
		return flattenedSubregions;
	}

	public boolean isPurchased() {
		return purchased;
	}

	public boolean isInPurchasedArea() {
		return isInPurchasedArea;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WorldRegion that = (WorldRegion) o;

		return !(name != null ? !name.toLowerCase().equals(that.name.toLowerCase()) : that.name != null);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	private WorldRegion() {
		superregion = null;
		subregions = new LinkedList<>();
		flattenedSubregions = new LinkedList<>();
	}

	private WorldRegion initWorld() {
		regionId = null;
		downloadsIdPrefix = "world_";
		name = null;
		superregion = null;

		return this;
	}

	private WorldRegion init(String regionId, OsmandRegions osmandRegions, String name) {
		this.regionId = regionId;
		String downloadName = osmandRegions.getDownloadName(regionId);
		if (downloadName != null) {
			downloadsIdPrefix = downloadName + ".";
			if (name != null) {
				this.name = name;
			} else {
				this.name = osmandRegions.getLocaleName(downloadName);
			}
		} else {
			this.downloadsIdPrefix = regionId + ".";
			this.name = name;
		}
		return this;
	}

	private WorldRegion init(String regionId, OsmandRegions osmandRegions) {
		this.regionId = regionId;
		String downloadName = osmandRegions.getDownloadName(regionId);
		if (downloadName != null) {
			downloadsIdPrefix = downloadName + ".";
			this.name = osmandRegions.getLocaleName(regionId);
		} else {
			this.downloadsIdPrefix = regionId + ".";
			this.name = regionId;
		}
		return this;
	}

	private WorldRegion init(String regionId, String name) {
		this.regionId = regionId;
		this.downloadsIdPrefix = regionId + ".";
		this.name = name;
		return this;
	}

	private void addSubregion(WorldRegion subregion) {
		subregion.superregion = this;
		subregions.add(subregion);
		propagateSubregionToFlattenedHierarchy(subregion);
	}

	private void propagateSubregionToFlattenedHierarchy(WorldRegion subregion) {
		flattenedSubregions.add(subregion);
		if (superregion != null) {
			superregion.propagateSubregionToFlattenedHierarchy(subregion);
		}
	}

	public static WorldRegion loadWorldRegions(OsmandApplication app) {
		OsmandRegions osmandRegions = app.getRegions();

		Map<String, String> loadedItems = osmandRegions.getFullNamesToLowercaseCopy();
		if (loadedItems.size() == 0) {
			return null;
		}

		HashMap<String, WorldRegion> regionsLookupTable = new HashMap<>(loadedItems.size());

		// Create root region
		WorldRegion entireWorld = new WorldRegion().initWorld();

		// Create main regions
		Resources res = app.getResources();

		WorldRegion africaRegion = createRegionAs(AFRICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_africa));
		entireWorld.addSubregion(africaRegion);
		regionsLookupTable.put(africaRegion.regionId, africaRegion);

		WorldRegion asiaRegion = createRegionAs(ASIA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_asia));
		entireWorld.addSubregion(asiaRegion);
		regionsLookupTable.put(asiaRegion.regionId, asiaRegion);

		WorldRegion australiaAndOceaniaRegion = createRegionAs(AUSTRALIA_AND_OCEANIA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_oceania));
		entireWorld.addSubregion(australiaAndOceaniaRegion);
		regionsLookupTable.put(australiaAndOceaniaRegion.regionId, australiaAndOceaniaRegion);

		WorldRegion centralAmericaRegion = createRegionAs(CENTRAL_AMERICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_central_america));
		entireWorld.addSubregion(centralAmericaRegion);
		regionsLookupTable.put(centralAmericaRegion.regionId, centralAmericaRegion);

		WorldRegion europeRegion = createRegionAs(EUROPE_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_europe));
		entireWorld.addSubregion(europeRegion);
		regionsLookupTable.put(europeRegion.regionId, europeRegion);

		WorldRegion northAmericaRegion = createRegionAs(NORTH_AMERICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_north_america));
		entireWorld.addSubregion(northAmericaRegion);
		regionsLookupTable.put(northAmericaRegion.regionId, northAmericaRegion);

		WorldRegion russiaRegion = createRegionAs(RUSSIA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_russia));
		entireWorld.addSubregion(russiaRegion);
		regionsLookupTable.put(russiaRegion.regionId, russiaRegion);

		WorldRegion southAmericaRegion = createRegionAs(SOUTH_AMERICA_REGION_ID,
				loadedItems, osmandRegions, res.getString(R.string.index_name_south_america));
		entireWorld.addSubregion(southAmericaRegion);
		regionsLookupTable.put(southAmericaRegion.regionId, southAmericaRegion);

		// Process remaining regions
		for (;;) {
			int processedRegions = 0;

			Iterator<Entry<String, String>> iterator = loadedItems.entrySet().iterator();
			while (iterator.hasNext()) {
				String regionId = iterator.next().getKey();
				String parentRegionId = osmandRegions.getParentFullName(regionId);
				if (parentRegionId == null) {
					continue;
				}

				// Try to find parent of this region
				WorldRegion parentRegion = regionsLookupTable.get(parentRegionId);
				if (parentRegion == null) {
					continue;
				}

				WorldRegion newRegion = new WorldRegion().init(regionId, osmandRegions);
				parentRegion.addSubregion(newRegion);
				regionsLookupTable.put(newRegion.regionId, newRegion);

				// Remove
				processedRegions++;
				iterator.remove();
			}

			// If all remaining are orphans, that's all
			if (processedRegions == 0)
				break;
		}

		LOG.warn("Found orphaned regions: " + loadedItems.size());
		for (String regionId : loadedItems.keySet()) {
			LOG.warn("FullName = " + regionId + " parent=" + osmandRegions.getParentFullName(regionId));
		}

		return entireWorld;
	}

	private static WorldRegion createRegionAs(String regionId, Map<String, String> loadedItems, OsmandRegions osmandRegions, String localizedName) {
		WorldRegion worldRegion;
		boolean hasRegion = loadedItems.containsKey(regionId);
		if (hasRegion) {
			worldRegion = new WorldRegion().init(regionId, osmandRegions, localizedName);
			loadedItems.remove(regionId);
		} else {
			worldRegion = new WorldRegion().init(regionId, localizedName);
		}
		return worldRegion;
	}
}