package org.openmaptiles.addons;

import static org.openmaptiles.util.Utils.nullIfEmpty;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.Translations;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

/**
 * Adds a {@code traffic_control} layer with the points a driver or walker meets on the road, named or not: traffic
 * signals, pedestrian crossings, level crossings (road over rail), stop and give-way signs, speed cameras, toll
 * gantries and speed bumps ({@code traffic_calming}).
 * <p>
 * Fields: {@code class} is {@code traffic_signals}, {@code crossing}, {@code level_crossing}, {@code rail_crossing},
 * {@code stop}, {@code give_way}, {@code speed_camera}, {@code toll_gantry} or {@code traffic_calming}.
 * {@code subclass} is the kind of crossing ({@code zebra}, {@code traffic_signals}, {@code uncontrolled}) or of speed
 * bump.
 */
public class TrafficControl implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  public static final String LAYER_NAME = "traffic_control";
  private static final int BUFFER_SIZE = 4;
  private static final Set<String> HIGHWAY_VALUES = Set.of(
    "traffic_signals", "crossing", "stop", "give_way", "speed_camera", "toll_gantry"
  );
  /** Small and dense, so they appear later than signals. */
  private static final Set<String> DENSE_CLASSES = Set.of("crossing", "traffic_calming", "stop", "give_way");

  public TrafficControl(Translations translations, PlanetilerConfig config, Stats stats) {}

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (!feature.isPoint()) {
      return;
    }
    String highway = nullIfEmpty(feature.getString("highway"));
    String railway = nullIfEmpty(feature.getString("railway"));
    String calming = nullIfEmpty(feature.getString("traffic_calming"));
    String clazz;
    String subclass = null;
    if (highway != null && HIGHWAY_VALUES.contains(highway)) {
      clazz = highway;
      if ("crossing".equals(highway)) {
        subclass = nullIfEmpty(feature.getString("crossing"));
      }
    } else if ("level_crossing".equals(railway)) {
      clazz = "level_crossing";
    } else if ("crossing".equals(railway)) {
      clazz = "rail_crossing";
    } else if (calming != null) {
      clazz = "traffic_calming";
      subclass = calming;
    } else {
      return;
    }
    features.point(LAYER_NAME)
      .setBufferPixels(BUFFER_SIZE)
      .setMinZoom(DENSE_CLASSES.contains(clazz) ? 15 : 14)
      .setAttr("class", clazz)
      .setAttr("subclass", subclass)
      .setAttr("direction", nullIfEmpty(feature.getString("direction")))
      .setAttr("name", nullIfEmpty(feature.getString("name")));
  }
}
