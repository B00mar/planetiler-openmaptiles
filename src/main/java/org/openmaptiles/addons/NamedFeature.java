package org.openmaptiles.addons;

import static org.openmaptiles.util.Utils.coalesce;
import static org.openmaptiles.util.Utils.nullIfEmpty;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.Translations;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;
import org.openmaptiles.util.OmtLanguageUtils;

/**
 * Adds a {@code named} layer with one point for every named OSM node and one label point (on the surface) for every
 * named polygon: shops, offices, clinics, gyms, named buildings, industrial and military areas, squares and so on.
 * <p>
 * The built-in layers only keep a fixed list of types, and only a few of them carry a name, so most of these would be
 * dropped. Some features end up here and in a built-in layer too (for example a hospital in {@code poi}); a style can
 * draw {@code poi} first and let label collision hide the repeat.
 * <p>
 * Skipped on purpose: names of one character (map-sheet markers like "A" and "B"), lines (rail, metro and roundabouts
 * have their own layers, roads are in {@code transportation_name}), and blood banks (own layer).
 * <p>
 * Fields: {@code kind} is the OSM key that decided the type (amenity, shop, building, ...) and {@code class} is its
 * value, plus names, operator, brand, phone, website, opening hours and address when the element has them.
 */
public class NamedFeature implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  public static final String LAYER_NAME = "named";
  private static final int BUFFER_SIZE = 4;
  /** The first of these keys found on an element becomes its {@code kind}. */
  private static final List<String> KINDS = List.of(
    "amenity", "shop", "healthcare", "tourism", "leisure", "office", "craft", "historic", "emergency",
    "public_transport", "man_made", "aeroway", "railway", "power", "barrier", "landuse", "natural", "place", "junction",
    "building", "boundary", "military", "sport", "waterway", "highway"
  );
  /** Big areas: their name is useful from a lower zoom. */
  private static final Set<String> AREA_KINDS = Set.of(
    "landuse", "natural", "place", "boundary", "military", "waterway"
  );
  private static final int AREA_MIN_ZOOM = 12;
  private static final int POI_MIN_ZOOM = 13;
  private static final int BUILDING_MIN_ZOOM = 15;

  private final Translations translations;

  public NamedFeature(Translations translations, PlanetilerConfig config, Stats stats) {
    this.translations = translations;
  }

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    String name = coalesce(
      nullIfEmpty(feature.getString("name")),
      nullIfEmpty(feature.getString("name:en")),
      nullIfEmpty(feature.getString("name:hi"))
    );
    if (name == null || name.strip().codePointCount(0, name.strip().length()) < 2 || BloodBank.isBloodBank(feature)) {
      return;
    }
    FeatureCollector.Feature output;
    if (feature.isPoint()) {
      output = features.point(LAYER_NAME);
    } else if (feature.canBePolygon()) {
      output = features.pointOnSurface(LAYER_NAME);
    } else {
      return;
    }

    String kind = KINDS.stream().filter(key -> nullIfEmpty(feature.getString(key)) != null).findFirst().orElse(null);
    Map<String, Object> names = OmtLanguageUtils.getNames(feature.tags(), translations);
    output
      .setBufferPixels(BUFFER_SIZE)
      .setMinZoom(minZoom(kind))
      .setAttr("kind", kind)
      .setAttr("class", kind == null ? null : feature.getString(kind))
      .setAttr("operator", nullIfEmpty(feature.getString("operator")))
      .setAttr("brand", nullIfEmpty(feature.getString("brand")))
      .setAttr("phone",
        coalesce(nullIfEmpty(feature.getString("phone")), nullIfEmpty(feature.getString("contact:phone"))))
      .setAttr("website",
        coalesce(nullIfEmpty(feature.getString("website")), nullIfEmpty(feature.getString("contact:website"))))
      .setAttr("opening_hours", nullIfEmpty(feature.getString("opening_hours")))
      .setAttr("addr:housenumber", nullIfEmpty(feature.getString("addr:housenumber")))
      .setAttr("addr:street", nullIfEmpty(feature.getString("addr:street")))
      .putAttrs(names);
    if (!names.containsKey("name")) {
      output.setAttr("name", name);
    }
  }

  private static int minZoom(String kind) {
    if (kind == null || AREA_KINDS.contains(kind)) {
      return AREA_MIN_ZOOM;
    }
    return "building".equals(kind) ? BUILDING_MIN_ZOOM : POI_MIN_ZOOM;
  }
}
