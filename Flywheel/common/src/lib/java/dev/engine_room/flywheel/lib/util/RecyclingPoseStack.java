package dev.engine_room.flywheel.lib.util;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * A {@link PoseStack} that recycles {@link PoseStack.Pose} objects.
 *
 * <p>Vanilla's {@link PoseStack} can get quite expensive to use when each game object needs to
 * maintain their own stack. This class helps alleviate memory pressure by making Pose objects
 * long-lived. Note that this means that you <em>CANNOT</em> safely store a Pose object outside
 * the RecyclingPoseStack that created it.
 */
public class RecyclingPoseStack extends PoseStack {
}
