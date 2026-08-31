# Create: Adapted — Fix history

Short, user-facing summary of fixes included in each release.

## 0.98

- Restored the original Potato Cannon recoil: the weapon now moves slightly back and returns forward after firing.
- Removed the intermittent downward swing caused by competing use animations.
- Kept the weapon and hand movement synchronized during recoil.
- Fixed a dedicated-server crash when using a wrench on belts in the public build.
- Public builds now use the same current code as the verified full build and are checked before release.

## 0.97

- Fixed dedicated-server crashes involving packages, Repackagers, track placement, belts, and fluid or track interactions.
- Fixed package insertion from belts and funnels into Repackagers.
- Improved client/server separation and startup stability.
- Improved the completeness of the public external-assets build.

## 0.96

- Fixed the schematic preview sometimes continuing to show the previously selected schematic.
- The preview now refreshes immediately and clears obsolete data when the selection changes.

## 0.95

- Fixed AE2 recipes for milling Sky Stone, milling Fluix Crystal, and mixing Fluix Crystal.
- Updated the affected recipes for current AE2 compatibility.

## 0.94

- Added accurate selection of individual blocks inside moving contraptions.
- Restored goggles and hover overlays for blocks on moving contraptions, including Modular Accumulator charge information.
- Improved optional Jade integration for moving contraption blocks.
- Restored required Catnip and Flywheel components in the public external-assets build.
- Fixed AE2 recipe compatibility.

## 0.93

- Fixed occasional missing registered items, including the Small Cogwheel, after restarting the game.
- Restored the missing-assets installation screen in the public build.
- Fixed opening folders on Windows when the game path contains Cyrillic characters.
- Fixed a Flywheel issue that could freeze new-world creation.
- Fixed vertical belt placement and scrolling direction.
- Fixed Hand Crank flickering and duplicate rendering.
- Restored the Linked Controller activation animation and improved its hand position.

## 0.92

- Fixed missing, black, or angle-dependent faces on curved Create tracks when Flywheel is enabled.
- Restored correct cutout rendering and mipmapping for track materials.
- Improved Flywheel mesh stability, normals, buffer handling, and cleanup of removed kinetic visuals.
