package dimensionalflux.register;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.data.SuperAdmin;

import java.util.function.Supplier;

/** Data the mod attaches to game objects. */
public final class DataAttachments {

    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DimensionalFlux.MODID);

    /** The super admin flag of a player; the id is kept so saved players keep their flag. */
    public static final Supplier<AttachmentType<Boolean>> SUPER_ADMIN = REGISTER.register("player_data", SuperAdmin::attachment);

    private DataAttachments() {
    }
}
