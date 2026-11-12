package dimensionalflux.client.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Item model of a crystal that turns slowly around a vertical axis and floats up and down, used by the
 * Dimensional Configurator while it holds a configuration. The crystal is baked once per animation frame, each frame
 * with its own transform, and the frame shown follows the clock; the item is marked animated so inventory views
 * redraw it.
 */
public final class FloatingCrystalItemModel implements ItemModel {

    /** Frames baked for one full turn. */
    private static final int FRAMES = 120;

    private final ItemModel[] mFrames;
    private final long mCycleMillis;

    private FloatingCrystalItemModel(ItemModel[] frames, long cycleMillis) {
        mFrames = frames;
        mCycleMillis = cycleMillis;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner,
                       int seed) {
        int frame = (int) ((Util.getMillis() % mCycleMillis) * mFrames.length / mCycleMillis);
        output.setAnimated();
        output.appendModelIdentityElement(frame);
        mFrames[frame].update(output, item, resolver, displayContext, level, owner, seed);
    }

    /**
     * @param model       the crystal model
     * @param pivot       point the crystal turns around and floats from, in model pixels
     * @param turnSeconds time of one full turn
     * @param bobPixels   height of the float above and below the pivot, in model pixels
     * @param bobSeconds  time of one float up and down; one turn should hold a whole number of them
     */
    public record Unbaked(Identifier model, Vector3fc pivot, float turnSeconds, float bobPixels,
                          float bobSeconds) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
                ExtraCodecs.VECTOR3F.fieldOf("pivot").forGetter(Unbaked::pivot),
                Codec.floatRange(0.5f, 600f).optionalFieldOf("turn_seconds", 6f).forGetter(Unbaked::turnSeconds),
                Codec.floatRange(0f, 16f).optionalFieldOf("bob_pixels", 0f).forGetter(Unbaked::bobPixels),
                Codec.floatRange(0.1f, 600f).optionalFieldOf("bob_seconds", 3f).forGetter(Unbaked::bobSeconds)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolved = baker.getModel(model);
            TextureSlots slots = resolved.getTopTextureSlots();
            QuadCollection quads = resolved.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, slots);

            float px = pivot.x() / 16f, py = pivot.y() / 16f, pz = pivot.z() / 16f;
            ItemModel[] frames = new ItemModel[FRAMES];
            for (int f = 0; f < FRAMES; f++) {
                double progress = (double) f / FRAMES;
                double seconds = progress * turnSeconds;
                float angle = (float) (progress * Math.PI * 2);
                float bob = (float) (bobPixels * Math.sin(2 * Math.PI * seconds / bobSeconds)) / 16f;
                Matrix4f frame = new Matrix4f(transformation)
                        .translate(px, py + bob, pz)
                        .rotateY(angle)
                        .translate(-px, -py, -pz);
                frames[f] = new CuboidItemModelWrapper(List.of(), quads, properties, frame);
            }
            return new FloatingCrystalItemModel(frames, Math.round(turnSeconds * 1000));
        }
    }
}
