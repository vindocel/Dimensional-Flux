package dimensionalflux.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.common.util.DimensionalGuiStack;
import dimensionalflux.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TileDimensionalReceiver extends TileDimensionalConnector {

    private final DimensionalReceiverHandler mHandler = new DimensionalReceiverHandler();

    @Nullable
    private EnergyStorage mEnergyCap;

    public TileDimensionalReceiver(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.DIMENSIONAL_RECEIVER.get(), pos, state);
    }

    //// ANTENNA ANIMATION (client only, never saved or synced) \\

    // resting angle to either side, idle pause and movement length in ticks
    private static final float REST_YAW_MIN = 18f, REST_YAW_MAX = 34f;
    private static final int IDLE_MIN = 40, IDLE_MAX = 180;
    private static final int MOVE_MIN = 7, MOVE_MAX = 12;

    private net.minecraft.util.RandomSource mHeadRandom;
    private boolean mHeadInit;
    private final float[] mHeadFrom = new float[3], mHeadTo = new float[3]; // yaw, pitch, roll in degrees
    private float mMoveStart, mMoveDuration = 1, mNextMove;
    private boolean mGlanceBack;

    private boolean mWasActive;

    /**
     * Current antenna pose as {yaw, pitch, roll} in degrees. The antenna rests straight while the receiver is
     * idle. While it delivers energy on a network, each receiver runs its own random sequence of quick,
     * servo-like moves every few seconds, and returns to straight when the transfer stops.
     */
    public float[] getHeadPose(float partialTick) {
        float now = (level == null ? 0 : level.getGameTime()) + partialTick;
        if (!mHeadInit) {
            mHeadRandom = net.minecraft.util.RandomSource.create(worldPosition.asLong() * 31L + 7L);
            java.util.Arrays.fill(mHeadFrom, 0f);
            java.util.Arrays.fill(mHeadTo, 0f);
            mMoveStart = now;
            mNextMove = Float.MAX_VALUE;
            mHeadInit = true;
        }
        boolean active = isTransferring();
        if (active != mWasActive) {
            mWasActive = active;
            if (active) {
                mNextMove = now + 10 + mHeadRandom.nextInt(40);
            } else {
                // ease back to the straight rest pose and stay there
                float[] current = evaluatePose(now);
                System.arraycopy(current, 0, mHeadFrom, 0, 3);
                java.util.Arrays.fill(mHeadTo, 0f);
                mMoveStart = now;
                mMoveDuration = MOVE_MAX;
                mNextMove = Float.MAX_VALUE;
                mGlanceBack = false;
            }
        }
        if (active && now >= mNextMove) {
            startMove(now);
        }
        return evaluatePose(now);
    }

    private float[] evaluatePose(float now) {
        float t = net.minecraft.util.Mth.clamp((now - mMoveStart) / mMoveDuration, 0f, 1f);
        // the servos move in sequence: turn (yaw), then tilt (pitch), then the head roll
        float ty = easeOutBack(t);
        float tp = easeOutBack(net.minecraft.util.Mth.clamp((t - 0.25f) / 0.75f, 0f, 1f));
        float tr = easeOutBack(net.minecraft.util.Mth.clamp((t - 0.45f) / 0.55f, 0f, 1f));
        return new float[]{
                net.minecraft.util.Mth.lerp(ty, mHeadFrom[0], mHeadTo[0]),
                net.minecraft.util.Mth.lerp(tp, mHeadFrom[1], mHeadTo[1]),
                net.minecraft.util.Mth.lerp(tr, mHeadFrom[2], mHeadTo[2])
        };
    }

    // pose fragments the antenna chooses from at random; the same kind never plays twice in a row
    private static final int POSE_SIDE = 0, POSE_SIDE_TILT = 1, POSE_CENTER = 2, POSE_CENTER_TURN = 3,
            POSE_UP = 4, POSE_DOWN = 5, POSE_CENTER_TILT = 6, POSE_COUNT = 7;
    private static final int[] POSE_WEIGHTS = {5, 3, 2, 3, 2, 2, 2};
    private int mLastPose = POSE_CENTER;
    // a short glance returns to the previous pose before the next random choice
    private final float[] mReturnPose = new float[3];

    private void startMove(float now) {
        System.arraycopy(evaluatePose(now), 0, mHeadFrom, 0, 3);
        if (mGlanceBack) {
            System.arraycopy(mReturnPose, 0, mHeadTo, 0, 3);
            mGlanceBack = false;
        } else {
            int pose = pickPose();
            applyPose(pose);
            mLastPose = pose;
            // now and then, only glance at the new pose and come back
            if (mHeadRandom.nextInt(5) == 0) {
                System.arraycopy(mHeadFrom, 0, mReturnPose, 0, 3);
                mGlanceBack = true;
            }
        }
        mMoveStart = now;
        mMoveDuration = MOVE_MIN + mHeadRandom.nextInt(MOVE_MAX - MOVE_MIN + 1);
        int idle = mGlanceBack ? 15 + mHeadRandom.nextInt(30) : IDLE_MIN + mHeadRandom.nextInt(IDLE_MAX - IDLE_MIN);
        mNextMove = now + mMoveDuration + idle;
    }

    private int pickPose() {
        int total = 0;
        for (int i = 0; i < POSE_COUNT; i++) {
            if (i != mLastPose) total += POSE_WEIGHTS[i];
        }
        int roll = mHeadRandom.nextInt(total);
        for (int i = 0; i < POSE_COUNT; i++) {
            if (i == mLastPose) continue;
            roll -= POSE_WEIGHTS[i];
            if (roll < 0) return i;
        }
        return POSE_CENTER;
    }

    private float range(float min, float max) {
        return net.minecraft.util.Mth.lerp(mHeadRandom.nextFloat(), min, max);
    }

    private float sign() {
        return mHeadRandom.nextBoolean() ? 1f : -1f;
    }

    /** Sets the target {yaw, pitch, roll}; a positive pitch tilts the antenna toward model up, a negative one down. */
    private void applyPose(int pose) {
        switch (pose) {
            case POSE_SIDE -> {
                mHeadTo[0] = sign() * range(REST_YAW_MIN, REST_YAW_MAX);
                mHeadTo[1] = range(-6f, 6f);
                mHeadTo[2] = range(-3f, 3f);
            }
            case POSE_SIDE_TILT -> {
                mHeadTo[0] = sign() * range(REST_YAW_MIN, REST_YAW_MAX);
                mHeadTo[1] = range(-5f, 5f);
                mHeadTo[2] = sign() * range(8f, 16f);
            }
            case POSE_CENTER -> {
                mHeadTo[0] = range(-1.5f, 1.5f);
                mHeadTo[1] = range(-1.5f, 1.5f);
                mHeadTo[2] = 0f;
            }
            case POSE_CENTER_TURN -> {
                mHeadTo[0] = sign() * range(5f, 11f);
                mHeadTo[1] = range(-3f, 3f);
                mHeadTo[2] = range(-2f, 2f);
            }
            case POSE_UP -> {
                mHeadTo[0] = range(-8f, 8f);
                mHeadTo[1] = range(12f, 22f);
                mHeadTo[2] = range(-4f, 4f);
            }
            case POSE_DOWN -> {
                mHeadTo[0] = range(-8f, 8f);
                mHeadTo[1] = -range(10f, 18f);
                mHeadTo[2] = range(-4f, 4f);
            }
            default -> {
                mHeadTo[0] = range(-4f, 4f);
                mHeadTo[1] = range(-4f, 4f);
                mHeadTo[2] = sign() * range(10f, 18f);
            }
        }
        // only the down pose dips slightly below level
        net.minecraft.world.level.block.state.BlockState state = getBlockState();
        if (state.hasProperty(dimensionalflux.common.block.DimensionalConnectorBlock.FACING) &&
                state.getValue(dimensionalflux.common.block.DimensionalConnectorBlock.FACING).getAxis().isHorizontal()) {
            mHeadTo[1] = switch (pose) {
                case POSE_UP -> range(20f, 28f);
                case POSE_DOWN -> -range(2f, 6f);
                default -> range(6f, 16f);
            };
        }
    }

    /** Overshoots the target slightly and settles back, like a servo motor. */
    private static float easeOutBack(float x) {
        final float c1 = 1.9f, c3 = c1 + 1f;
        float k = x - 1f;
        return 1f + c3 * k * k * k + c1 * k * k;
    }

    @Nonnull
    @Override
    public DimensionalDeviceType getDeviceType() {
        return DimensionalDeviceType.RECEIVER;
    }

    @Nonnull
    @Override
    public DimensionalReceiverHandler getTransferHandler() {
        return mHandler;
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        return DimensionalGuiStack.DIMENSIONAL_RECEIVER;
    }

    @Override
    @SuppressWarnings("NonExtendableApiUsage")
    public void invalidateCapabilities() {
        mEnergyCap = null;
        super.invalidateCapabilities();
    }

    @Nullable
    @Override
    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (isRemoved() || side == null || side != getMountingSide()) {
            return null;
        }
        if (mEnergyCap == null) {
            mEnergyCap = new EnergyStorage();
        }
        return mEnergyCap;
    }

    /**
     * A read-only view of the receiver buffer. The receiver pushes energy into the machine itself, so the handler
     * neither accepts nor gives energy to callers.
     */
    private class EnergyStorage implements EnergyHandler {

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmountAsLong() {
            return mHandler.getBuffer();
        }

        @Override
        public long getCapacityAsLong() {
            return Math.max(mHandler.getBuffer(), mHandler.getLimit());
        }
    }
}
