package net.createmod.catnip.api.client.gui.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelBatchRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelPartRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiFluidStateRenderState;
import net.createmod.catnip.api.platform.services.ModFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.TypedInstance;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class GuiGameElement {
	private static final ThreadLocal<ModelBatch> MODEL_BATCH = new ThreadLocal<>();

	public static void beginModelBatch() {
		beginModelBatch(0, 0, 0);
	}

	public static void beginModelBatch(float globalXRot, float globalYRot, float globalZRot) {
		beginModelBatch(globalXRot, globalYRot, globalZRot, false);
	}

	public static void beginBlockModelBatch(float globalXRot, float globalYRot, float globalZRot) {
		beginModelBatch(globalXRot, globalYRot, globalZRot, true);
	}

	private static void beginModelBatch(float globalXRot, float globalYRot, float globalZRot,
		boolean rotateAroundBlockCenter) {
		if (MODEL_BATCH.get() != null)
			throw new IllegalStateException("Nested GUI model batches are not supported");
		MODEL_BATCH.set(new ModelBatch(globalXRot, globalYRot, globalZRot, rotateAroundBlockCenter));
	}

	public static void endModelBatch(GuiGraphicsExtractor graphics) {
		ModelBatch batch = MODEL_BATCH.get();
		MODEL_BATCH.remove();
		if (batch == null || batch.entries.isEmpty() || batch.pose == null)
			return;
		int x0 = -80, y0 = -64, x1 = 80, y1 = 80;
		ScreenRectangle scissor = graphics.peekScissorStack();
		ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0)
			.transformMaxBounds(batch.pose);
		if (scissor != null)
			bounds = scissor.intersection(bounds);
		graphics.guiRenderState.addPicturesInPictureState(new GuiBlockModelBatchRenderState(
			List.copyOf(batch.entries), batch.pose, x0, y0, x1, y1, batch.scale,
			batch.globalXRot, batch.globalYRot, batch.globalZRot, batch.rotateAroundBlockCenter,
			scissor, bounds));
	}

	public static void submitFluidBox(TypedInstance<Fluid> fluid,
		float xLocal, float yLocal, float zLocal, float localScale,
		float postX, float postY, float postZ,
		float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		ModelBatch batch = MODEL_BATCH.get();
		if (batch == null)
			throw new IllegalStateException("GUI fluid boxes must be submitted inside a model batch");
		batch.entries.add(new GuiBlockModelBatchRenderState.Entry(null, null, fluid,
			xLocal, yLocal, zLocal, 0, 0, 0, localScale,
			postX, postY, postZ, minX, minY, minZ, maxX, maxY, maxZ, false, 0xffffffff));
	}

	private static class ModelBatch {
		private final List<GuiBlockModelBatchRenderState.Entry> entries = new ArrayList<>();
		private Matrix3x2f pose;
		private float scale;
		private final float globalXRot;
		private final float globalYRot;
		private final float globalZRot;
		private final boolean rotateAroundBlockCenter;

		private ModelBatch(float globalXRot, float globalYRot, float globalZRot,
			boolean rotateAroundBlockCenter) {
			this.globalXRot = globalXRot;
			this.globalYRot = globalYRot;
			this.globalZRot = globalZRot;
			this.rotateAroundBlockCenter = rotateAroundBlockCenter;
		}

		private void add(Matrix3x2f pose, float scale, GuiBlockModelBatchRenderState.Entry entry) {
			if (this.pose == null) {
				this.pose = pose;
				this.scale = scale;
			}
			entries.add(entry);
		}
	}
    public static GuiRenderBuilder of(ItemStack stack) {
        return new GuiItemRenderBuilder(stack);
    }

    public static GuiRenderBuilder of(ItemLike itemProvider) {
        return new GuiItemRenderBuilder(itemProvider);
    }

    public static GuiRenderBuilder of(BlockState state) {
        return new GuiBlockStateRenderBuilder(state);
    }

    public static GuiRenderBuilder of(BlockStateModelPart part) {
        return new GuiBlockModelPartRenderBuilder(part);
    }

    public static GuiRenderBuilder of(BlockState state, @Nullable BlockEntity blockEntity) {
        return new GuiBlockEntityRenderBuilder(state, blockEntity);
    }

    public static GuiRenderBuilder of(BlockEntity blockEntity) {
        return of(blockEntity.getBlockState(), blockEntity);
    }

    public static GuiRenderBuilder of(Fluid fluid) {
        return new GuiBlockStateRenderBuilder(
                fluid.defaultFluidState().createLegacyBlock().setValue(LiquidBlock.LEVEL, 0));
    }

    public abstract static class GuiRenderBuilder extends AbstractRenderElement {
        protected float xLocal, yLocal, zLocal;
        protected double xRot, yRot, zRot;
        protected double scale = 1;
        protected int color = 0xFFFFFF;
		protected boolean cullBackFaces;
        protected Vector2f rotationOffset = new Vector2f();

        @Nullable
        protected ILightingSettings customLighting = null;

        @Override
        public GuiRenderBuilder at(float x, float y) {
            super.at(x, y);
            return this;
        }

        @Override
        public GuiRenderBuilder at(float x, float y, float z) {
            super.at(x, y, z);
            return this;
        }

        @Override
        public GuiRenderBuilder withBounds(int width, int height) {
            super.withBounds(width, height);
            return this;
        }

        @Override
        public GuiRenderBuilder withAlpha(float alpha) {
            super.withAlpha(alpha);
            return this;
        }

        public GuiRenderBuilder atLocal(float x, float y, float z) {
            this.xLocal = x;
            this.yLocal = y;
            this.zLocal = z;
            return this;
        }

        public GuiRenderBuilder rotate(double xRot, double yRot, double zRot) {
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
            return this;
        }

        public GuiRenderBuilder rotateBlock(double xRot, double yRot, double zRot) {
            return this.rotate(xRot, yRot, zRot)
                    .withRotationOffset(new Vector2f(0.5f, 0.5f));
        }

        public GuiRenderBuilder scale(double scale) {
            this.scale = scale;
            return this;
        }

        public GuiRenderBuilder color(int color) {
            this.color = color;
            return this;
        }

        public GuiRenderBuilder withRotationOffset(Vector2f offset) {
            this.rotationOffset = offset;
            return this;
        }

        public GuiRenderBuilder lighting(ILightingSettings lighting) {
            customLighting = lighting;
            return this;
        }

        protected void prepareMatrix(Matrix3x2fStack poseStack) {
            poseStack.pushMatrix();
            // RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            // RenderSystem.enableDepthTest();
            // RenderSystem.enableBlend();
            // RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            prepareLighting();
        }

        protected void transformMatrix(Matrix3x2fStack poseStack) {
			poseStack.translate(x, y);
			poseStack.scale((float) scale, (float) scale);
			poseStack.translate(xLocal, yLocal);
			UIRenderHelper.flipForGuiRender(poseStack);
			poseStack.translate(rotationOffset.x, rotationOffset.y);
			// TODO: how
//            poseStack.mulPose(Axis.ZP.rotationDegrees((float) zRot));
//            poseStack.mulPose(Axis.XP.rotationDegrees((float) xRot));
//            poseStack.mulPose(Axis.YP.rotationDegrees((float) yRot));
            poseStack.translate(-rotationOffset.x, -rotationOffset.y);
        }

		public GuiRenderBuilder cullBackFaces() {
			this.cullBackFaces = true;
			return this;
		}

        /**
         * PIP models are scaled in their 3D render target, not by stretching the finished
         * GUI texture. Local offsets remain screen-aligned, matching the old PoseStack API.
         */
        protected void transformPipMatrix(Matrix3x2fStack poseStack) {
            poseStack.translate(x, y);
        }

        @Nullable
        protected ScreenRectangle pipBounds(Matrix3x2f pose, int x0, int y0, int x1, int y1,
                                             @Nullable ScreenRectangle scissor) {
            ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0)
                    .transformMaxBounds(pose);
            return scissor == null ? bounds : scissor.intersection(bounds);
        }

        protected void cleanUpMatrix(Matrix3x2fStack poseStack) {
            poseStack.popMatrix();
            cleanUpLighting();
        }

        protected void prepareLighting() {
            Objects.requireNonNullElse(customLighting, ILightingSettings.ITEMS_3D)
                    .apply();
        }

        protected void cleanUpLighting() {
            if (customLighting != null) {
                ILightingSettings.ITEMS_3D.apply();
            }
        }
    }

    protected static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {
        protected BlockStateModel blockStateModel;
        protected BlockState blockState;

        @Nullable
        protected BlockEntity blockEntity;

        public GuiBlockModelRenderBuilder(
                BlockStateModel blockStateModel,
                @Nullable BlockState blockState,
                @Nullable BlockEntity blockEntity) {
            this.blockState = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
            this.blockStateModel = blockStateModel;
            this.blockEntity = blockEntity;
        }

        @Override
        public void submit(GuiGraphicsExtractor graphics) {
			Matrix3x2fStack poseStack = graphics.pose();
			prepareMatrix(poseStack);
			transformPipMatrix(poseStack);

			submitModel(graphics);

			cleanUpMatrix(poseStack);
        }

        protected void submitModel(GuiGraphicsExtractor graphics) {
			ModelBatch batch = MODEL_BATCH.get();
			if (batch != null) {
				batch.add(new Matrix3x2f(graphics.pose()), (float) scale,
					new GuiBlockModelBatchRenderState.Entry(blockState, null, null,
						xLocal, yLocal, zLocal, (float) xRot, (float) yRot, (float) zRot,
						1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						cullBackFaces, ARGB.color(255, color)));
				return;
			}
			Matrix3x2f pose = new Matrix3x2f(graphics.pose());
			ScreenRectangle scissor = graphics.peekScissorStack();
			int x0 = -64, y0 = -64, x1 = 64, y1 = 80;
			graphics.guiRenderState.addPicturesInPictureState(
				new GuiBlockModelRenderState(blockState, blockEntity,
					pose,
					xLocal, yLocal, zLocal,
					(float) xRot, (float) yRot, (float) zRot,
					ARGB.color(255, color),
					x0, y0, x1, y1, (float) scale,
					scissor, pipBounds(pose, x0, y0, x1, y1, scissor)
				)
			);
        }
    }

    public static class GuiBlockModelPartRenderBuilder extends GuiRenderBuilder {
        private final BlockStateModelPart part;

        public GuiBlockModelPartRenderBuilder(BlockStateModelPart part) {
            this.part = part;
        }

        @Override
        public void submit(GuiGraphicsExtractor graphics) {
            Matrix3x2fStack poseStack = graphics.pose();
            prepareMatrix(poseStack);
			transformPipMatrix(poseStack);
			Matrix3x2f pose = new Matrix3x2f(graphics.pose());
			ModelBatch batch = MODEL_BATCH.get();
			if (batch != null) {
				batch.add(pose, (float) scale, new GuiBlockModelBatchRenderState.Entry(null, part, null,
					xLocal, yLocal, zLocal, (float) xRot, (float) yRot, (float) zRot,
					1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					cullBackFaces, ARGB.color(255, color)));
				cleanUpMatrix(poseStack);
				return;
			}
			ScreenRectangle scissor = graphics.peekScissorStack();
			int x0 = -64, y0 = -64, x1 = 64, y1 = 80;
            graphics.guiRenderState.addPicturesInPictureState(new GuiBlockModelPartRenderState(part,
                pose, xLocal, yLocal, zLocal, (float) xRot, (float) yRot, (float) zRot,
                ARGB.color(255, color), x0, y0, x1, y1, (float) scale,
				scissor, pipBounds(pose, x0, y0, x1, y1, scissor)));
            cleanUpMatrix(poseStack);
        }
    }

    public static class GuiBlockEntityRenderBuilder extends GuiBlockModelRenderBuilder {
        public GuiBlockEntityRenderBuilder(
                BlockState blockState, @Nullable BlockEntity blockEntity) {
            super(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState), blockState, blockEntity);
        }

    }

    public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {
        public GuiBlockStateRenderBuilder(BlockState blockstate) {
            super(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockstate), blockstate, null);
        }

        @Override
        protected void submitModel(GuiGraphicsExtractor graphics) {
            if (blockState.getBlock() instanceof BaseFireBlock) {
                ILightingSettings.ITEMS_FLAT.apply();
                super.submitModel(graphics);
                ILightingSettings.ITEMS_3D.apply();
                return;
            }

            super.submitModel(graphics);

			if (blockState.getFluidState().isEmpty()) return;
			if (MODEL_BATCH.get() != null) {
				TypedInstance<Fluid> fluid = ModFluidHelper.INSTANCE.instanceFor(blockState.getFluidState());
				submitFluidBox(fluid, xLocal, yLocal, zLocal, 1,
					0, 0, 0, 0, 0, 0, 1, 1, 1);
				return;
			}

			graphics.guiRenderState.addPicturesInPictureState(new GuiFluidStateRenderState(blockState.getFluidState(), new Matrix3x2f(graphics.pose()),
				0, 0, 16, 16, 1, null, null));
        }
    }

    public static class GuiItemRenderBuilder extends GuiRenderBuilder {
        private final ItemStack stack;

        public GuiItemRenderBuilder(ItemStack stack) {
            this.stack = stack;
        }

        public GuiItemRenderBuilder(ItemLike provider) {
            this(new ItemStack(provider));
        }

        @Override
        public void submit(GuiGraphicsExtractor graphics) {
             Matrix3x2fStack poseStack = graphics.pose();
             prepareMatrix(poseStack);
             transformMatrix(poseStack);
			 graphics.item(this.stack, 0, 0);
             cleanUpMatrix(poseStack);
        }
    }
}
