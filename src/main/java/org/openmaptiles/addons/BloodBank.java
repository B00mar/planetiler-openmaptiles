package org.openmaptiles.addons;

import static org.openmaptiles.util.Utils.coalesce;
import static org.openmaptiles.util.Utils.nullIfEmpty;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.Translations;
import java.util.Map;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;
import org.openmaptiles.util.OmtLanguageUtils;

/**
 * Adds a {@code blood_bank} layer with one point for every blood bank or blood donation site. The built-in {@code poi}
 * layer drops these because they are not in its list of allowed {@code amenity} values.
 * <p>
 * Matches {@code amenity=blood_bank}, {@code amenity=blood_donation}, {@code healthcare=blood_bank} and
 * {@code healthcare=blood_donation}.
 */
public class BloodBank implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  public static final String LAYER_NAME = "blood_bank";
  private static final int BUFFER_SIZE = 4;
  private static final int MIN_ZOOM = 10;

  private final Translations translations;

  public BloodBank(Translations translations, PlanetilerConfig config, Stats stats) {
    this.translations = translations;
  }

  @Override
  public String name() {
    return LAYER_NAME;
  }

  static boolean isBloodBank(SourceFeature feature) {
    return feature.hasTag("amenity", "blood_bank", "blood_donation") ||
      feature.hasTag("healthcare", "blood_bank", "blood_donation");
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (!isBloodBank(feature)) {
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
    // like ATMs in the poi layer: when there is no name, fall back to the operator so the pin is still labelled
    Map<String, Object> names = OmtLanguageUtils.getNames(feature.tags(), translations);
    String operator = nullIfEmpty(feature.getString("operator"));
    output
      .setBufferPixels(BUFFER_SIZE)
      .setMinZoom(MIN_ZOOM)
      .setAttr("class", LAYER_NAME)
      .setAttr("operator", operator)
      .setAttr("phone",
        coalesce(nullIfEmpty(feature.getString("phone")), nullIfEmpty(feature.getString("contact:phone"))))
      .setAttr("website",
        coalesce(nullIfEmpty(feature.getString("website")), nullIfEmpty(feature.getString("contact:website"))))
      .setAttr("opening_hours", nullIfEmpty(feature.getString("opening_hours")))
      .setAttr("addr:housenumber", nullIfEmpty(feature.getString("addr:housenumber")))
      .setAttr("addr:street", nullIfEmpty(feature.getString("addr:street")))
      .putAttrs(names);
    if (!names.containsKey("name") && operator != null) {
      output.setAttr("name", operator);
    }
  }
}
