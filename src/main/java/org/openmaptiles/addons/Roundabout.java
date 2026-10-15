package org.openmaptiles.addons;

import static org.openmaptiles.util.Utils.nullIfEmpty;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.Translations;
import org.locationtech.jts.geom.LineString;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;
import org.openmaptiles.util.OmtLanguageUtils;

/**
 * Adds a {@code roundabout} layer with every roundabout and traffic circle, named or not:
 * <ul>
 * <li>{@code junction=roundabout} and {@code junction=circular} ways as lines (the ring of the roundabout)</li>
 * <li>one point in the middle of every closed ring, to hang an icon or a name label on</li>
 * <li>{@code highway=mini_roundabout} nodes as points</li>
 * </ul>
 * The {@code class} field is {@code roundabout}, {@code circular} or {@code mini_roundabout}.
 */
public class Roundabout implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  public static final String LAYER_NAME = "roundabout";
  private static final int BUFFER_SIZE = 4;

  private final Translations translations;
  private final Stats stats;

  public Roundabout(Translations translations, PlanetilerConfig config, Stats stats) {
    this.translations = translations;
    this.stats = stats;
  }

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (feature.isPoint()) {
      if (feature.hasTag("highway", "mini_roundabout")) {
        addAttributes(features.point(LAYER_NAME), feature, "mini_roundabout");
      }
    } else if (feature.canBeLine() && feature.hasTag("junction", "roundabout", "circular")) {
      String clazz = feature.getString("junction");
      addAttributes(features.line(LAYER_NAME), feature, clazz);
      if (isClosed(feature)) {
        addAttributes(features.centroid(LAYER_NAME), feature, clazz);
      }
    }
  }

  private boolean isClosed(SourceFeature feature) {
    try {
      return feature.line()instanceof LineString line && line.isClosed();
    } catch (GeometryException e) {
      e.log(stats, "roundabout_geometry", "Unable to read roundabout geometry " + feature.id());
      return false;
    }
  }

  private void addAttributes(FeatureCollector.Feature output, SourceFeature feature, String clazz) {
    output
      .setBufferPixels(BUFFER_SIZE)
      .setAttr("class", clazz)
      .setAttr("highway", nullIfEmpty(feature.getString("highway")))
      .setAttr("ref", nullIfEmpty(feature.getString("ref")))
      .putAttrs(OmtLanguageUtils.getNames(feature.tags(), translations));
  }
}
