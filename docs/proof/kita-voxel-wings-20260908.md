# Kita voxel wings

Replaced the transparent wing planes with mirrored solid cuboid feathers.
Each wing has a shoulder and an articulated outer section, an ascending arch,
stepped flight-feather tips, and two overlapping rows of smaller cover feathers.
The wings follow the torso transform and slowly move at both joints.
The material uses Minecraft's snow pixel texture with normal entity lighting.
The original player skin and gold halo are unchanged.

Captured in the actual Fabric client using:

`gradlew.bat --no-daemon runClient -PbossVisualProof`

The repository window mixin hides the proof window; the proof controller mutes
all sound categories. This run captured a stationary boss with normal renderer
animation, at 50-degree FOV. It does not test combat movement.
The proof now waits for resource loading before staging the world.

Inspected all four original framebuffer captures:

- `kita-voxel-wings-front-20260908.png` (22:47:20)
- `kita-voxel-wings-side-20260908.png` (22:47:28)
- `kita-voxel-wings-back-20260908.png` (22:47:36)
- `kita-voxel-wings-three-quarter-20260908.png` (22:47:44)

The wings remain attached behind the shoulders, have visible side thickness,
and retain a stepped feather silhouette in all four views. Normal directional
lighting shades the white feathers gray on the faces turned away from light.
These captures supersede the earlier flat-wing captures for wing appearance.
