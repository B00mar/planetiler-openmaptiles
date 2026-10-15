package org.openmaptiles.layers;

import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.stats.Stats;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openmaptiles.OpenMapTilesProfile;

class ExtraLayersTest extends AbstractLayerTest {

  @Override
  OpenMapTilesProfile createProfile() {
    return new OpenMapTilesProfile(translations, PlanetilerConfig.defaults(), Stats.inMemory());
  }

  @Test
  void namedRailLineIsKept() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "railway",
      "_type", "line",
      "class", "rail",
      "name", "BPL - MKC Rail Section"
    )), only("railway", process(lineFeature(Map.of(
      "railway", "rail",
      "name", "BPL - MKC Rail Section"
    )))));
  }

  @Test
  void unnamedRailLineIsLeftToTransportation() {
    assertFeatures(12, List.of(), only("railway", process(lineFeature(Map.of(
      "railway", "rail"
    )))));
  }

  @Test
  void metroLinesAreKeptEvenWithoutName() {
    for (String clazz : List.of("subway", "light_rail", "construction", "proposed")) {
      assertFeatures(12, List.of(Map.of(
        "_layer", "railway",
        "_type", "line",
        "class", clazz
      )), only("railway", process(lineFeature(Map.of(
        "railway", clazz
      )))));
    }
  }

  @Test
  void metroUnderConstructionKeepsPlannedKind() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "railway",
      "class", "construction",
      "construction", "subway",
      "name", "Line-2 (Purple Line)",
      "operator", "Madhya Pradesh Metro Rail Corporation"
    )), only("railway", process(lineFeature(Map.of(
      "railway", "construction",
      "construction", "subway",
      "name", "Line-2 (Purple Line)",
      "operator", "Madhya Pradesh Metro Rail Corporation"
    )))));
  }

  @Test
  void bloodBankPoint() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "blood_bank",
      "_type", "point",
      "_minzoom", 10,
      "class", "blood_bank",
      "name", "Arpan Blood Bank",
      "phone", "123"
    )), only("blood_bank", process(pointFeature(Map.of(
      "amenity", "blood_bank",
      "name", "Arpan Blood Bank",
      "phone", "123"
    )))));
  }

  @Test
  void bloodDonationTagsAreAlsoKept() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "blood_bank",
      "_type", "point"
    )), only("blood_bank", process(pointFeature(Map.of(
      "healthcare", "blood_donation"
    )))));
  }

  @Test
  void bloodBankWithoutNameUsesOperator() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "blood_bank",
      "name", "Red Cross"
    )), only("blood_bank", process(pointFeature(Map.of(
      "amenity", "blood_bank",
      "operator", "Red Cross"
    )))));
  }

  @Test
  void bloodBankPolygonBecomesPoint() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "blood_bank",
      "_type", "point",
      "name", "City Blood Bank"
    )), only("blood_bank", process(polygonFeature(Map.of(
      "amenity", "blood_bank",
      "name", "City Blood Bank"
    )))));
  }

  @Test
  void roundaboutRingGetsLineAndCenterPoint() {
    assertFeatures(14, List.of(
      Map.of("_layer", "roundabout", "_type", "line", "class", "roundabout", "highway", "primary", "name",
        "Prabhat Square"),
      Map.of("_layer", "roundabout", "_type", "point", "class", "roundabout", "name", "Prabhat Square")
    ), only("roundabout", process(closedWayFeature(Map.of(
      "junction", "roundabout",
      "highway", "primary",
      "name", "Prabhat Square"
    )))));
  }

  @Test
  void unnamedRoundaboutIsKept() {
    assertFeatures(14, List.of(
      Map.of("_layer", "roundabout", "_type", "line", "class", "roundabout"),
      Map.of("_layer", "roundabout", "_type", "point", "class", "roundabout")
    ), only("roundabout", process(closedWayFeature(Map.of(
      "junction", "roundabout",
      "highway", "residential"
    )))));
  }

  @Test
  void miniRoundaboutPoint() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "roundabout",
      "_type", "point",
      "class", "mini_roundabout"
    )), only("roundabout", process(pointFeature(Map.of(
      "highway", "mini_roundabout"
    )))));
  }

  @Test
  void trafficCircleCountsAsRoundabout() {
    assertFeatures(14, List.of(
      Map.of("_layer", "roundabout", "_type", "line", "class", "circular"),
      Map.of("_layer", "roundabout", "_type", "point", "class", "circular")
    ), only("roundabout", process(closedWayFeature(Map.of(
      "junction", "circular",
      "highway", "tertiary"
    )))));
  }

  @Test
  void ordinaryRoadIsNotARoundabout() {
    assertFeatures(14, List.of(), only("roundabout", process(lineFeature(Map.of(
      "highway", "primary"
    )))));
  }

  @Test
  void namedShopNodeIsKept() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "named",
      "_type", "point",
      "_minzoom", 13,
      "kind", "shop",
      "class", "yes",
      "name", "Jain Kirana",
      "phone", "123"
    )), only("named", process(pointFeature(Map.of(
      "shop", "yes",
      "name", "Jain Kirana",
      "phone", "123"
    )))));
  }

  @Test
  void namedBuildingPolygonBecomesLabelPointFromZoom15() {
    assertFeatures(15, List.of(Map.of(
      "_layer", "named",
      "_type", "point",
      "_minzoom", 15,
      "kind", "building",
      "class", "university",
      "name", "Academic Block"
    )), only("named", process(polygonFeature(Map.of(
      "building", "university",
      "name", "Academic Block"
    )))));
  }

  @Test
  void namedLandusePointShowsFromZoom12() {
    assertFeatures(12, List.of(Map.of(
      "_layer", "named",
      "_minzoom", 12,
      "kind", "landuse",
      "class", "military",
      "name", "Sultania Infantry"
    )), only("named", process(polygonFeature(Map.of(
      "landuse", "military",
      "name", "Sultania Infantry"
    )))));
  }

  @Test
  void namedNodeWithoutKnownKeyStillKept() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "named",
      "name", "Chanchal choraha"
    )), only("named", process(pointFeature(Map.of(
      "name", "Chanchal choraha"
    )))));
  }

  @Test
  void onlyEnglishNameIsEnough() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "named",
      "name", "Fitness Hub"
    )), only("named", process(pointFeature(Map.of(
      "leisure", "fitness_centre",
      "name:en", "Fitness Hub"
    )))));
  }

  @Test
  void unnamedAndOneCharacterNamesAreSkipped() {
    assertFeatures(14, List.of(), only("named", process(pointFeature(Map.of("shop", "yes")))));
    assertFeatures(14, List.of(), only("named", process(pointFeature(Map.of("place", "locality", "name", "A")))));
    assertFeatures(14, List.of(), only("named", process(pointFeature(Map.of("shop", "yes", "name", " ")))));
  }

  @Test
  void namedLinesAreLeftToOtherLayers() {
    assertFeatures(14, List.of(), only("named", process(lineFeature(Map.of(
      "waterway", "stream",
      "name", "Some Nala"
    )))));
  }

  @Test
  void namedBloodBankIsNotRepeatedInNamedLayer() {
    assertFeatures(14, List.of(), only("named", process(pointFeature(Map.of(
      "amenity", "blood_bank",
      "name", "Arpan Blood Bank"
    )))));
  }

  @Test
  void trafficSignals() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "traffic_control",
      "_type", "point",
      "_minzoom", 14,
      "class", "traffic_signals"
    )), only("traffic_control", process(pointFeature(Map.of(
      "highway", "traffic_signals"
    )))));
  }

  @Test
  void pedestrianCrossingKeepsItsKind() {
    assertFeatures(15, List.of(Map.of(
      "_layer", "traffic_control",
      "_minzoom", 15,
      "class", "crossing",
      "subclass", "zebra"
    )), only("traffic_control", process(pointFeature(Map.of(
      "highway", "crossing",
      "crossing", "zebra"
    )))));
  }

  @Test
  void levelCrossingAndRailCrossing() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "traffic_control",
      "_minzoom", 14,
      "class", "level_crossing"
    )), only("traffic_control", process(pointFeature(Map.of("railway", "level_crossing")))));
    assertFeatures(14, List.of(Map.of(
      "_layer", "traffic_control",
      "class", "rail_crossing"
    )), only("traffic_control", process(pointFeature(Map.of("railway", "crossing")))));
  }

  @Test
  void speedBump() {
    assertFeatures(15, List.of(Map.of(
      "_layer", "traffic_control",
      "class", "traffic_calming",
      "subclass", "bump"
    )), only("traffic_control", process(pointFeature(Map.of("traffic_calming", "bump")))));
  }

  @Test
  void otherHighwayPointsAndLinesAreNotTrafficControl() {
    assertFeatures(15, List.of(), only("traffic_control", process(pointFeature(Map.of("highway", "bus_stop")))));
    assertFeatures(15, List.of(), only("traffic_control", process(lineFeature(Map.of("highway", "crossing")))));
  }
}
