package dimensionalflux.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Dimensional Flux Guide: using it opens the guide screen on the client. */
public class GuideBookItem extends Item {

    /** Opens the guide; set by the client, does nothing on a dedicated server. */
    public static Runnable sOpenGuide = () -> {
    };

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            sOpenGuide.run();
        }
        return InteractionResult.SUCCESS;
    }
}
