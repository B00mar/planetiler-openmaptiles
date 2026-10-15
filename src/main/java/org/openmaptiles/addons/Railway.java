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
import org.openmaptiles.util.OmtLanguageUtils;

/**
 * Adds a {@code railway} layer with named railway lines and every metro / light rail line, including the ones that are
 * still under construction or only proposed.
 * <p>
 * The {@code transportation} layer already draws railways, but it has no names. This layer carries the names, refs and
 * operators so a style can label the lines.
 */
public class Railway implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  public static final String LAYER_NAME = "railway";
  private static final int BUFFER_SIZE = 4;
  /** Railway values that are drawn as a line when they have a name. */
  private static final Set<String> TRACKS = Set.of(
    "rail", "light_rail", "subway", "tram", "monorail", "narrow_gauge", "funicular", "preserved", "construction",
    "proposed"
  );
  /** Metro-like values that are kept even when they have no name. */
  private static final Set<String> METRO = Set.of("subway", "light_rail", "monorail", "construction", "proposed");

  private final Translations translations;

  public Railway(Translations translations, PlanetilerConfig config, Stats stats) {
    this.translations = translations;
  }

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    String railway = nullIfEmpty(feature.getString("railway"));
    if (railway == null || !feature.canBeLine() || !TRACKS.contains(railway)) {
      return;
    }
    boolean named = nullIfEmpty(feature.getString("name")) != null;
    if (!named && !METRO.contains(railway)) {
      return;
    }
    features.line(LAYER_NAME)
      .setBufferPixels(BUFFER_SIZE)
      .setAttr("class", railway)
      // railway=construction / proposed keep the planned kind in construction=* / proposed=*
      .setAttr("construction", nullIfEmpty(feature.getString("construction")))
      .setAttr("proposed", nullIfEmpty(feature.getString("proposed")))
      .setAttr("ref", nullIfEmpty(feature.getString("ref")))
      .setAttr("operator", nullIfEmpty(feature.getString("operator")))
      .setAttr("network", nullIfEmpty(feature.getString("network")))
      .setAttr("usage", nullIfEmpty(feature.getString("usage")))
      .setAttr("service", nullIfEmpty(feature.getString("service")))
      .setAttr("tunnel", nullIfEmpty(feature.getString("tunnel")))
      .setAttr("bridge", nullIfEmpty(feature.getString("bridge")))
      .putAttrs(OmtLanguageUtils.getNames(feature.tags(), translations));
  }
}
