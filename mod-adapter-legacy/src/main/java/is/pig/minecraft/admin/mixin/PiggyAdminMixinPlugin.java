package is.pig.minecraft.admin.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class PiggyAdminMixinPlugin implements IMixinConfigPlugin {

    private boolean is1_21_2 = false;

    @Override
    public void onLoad(String mixinPackage) {
        FabricLoader.getInstance().getModContainer("minecraft").ifPresent(mod -> {
            String ver = mod.getMetadata().getVersion().getFriendlyString();
            if (ver.startsWith("1.21.2") || ver.startsWith("1.21.3") || ver.startsWith("1.21.4") || ver.startsWith("26")) {
                is1_21_2 = true;
            }
        });
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ExplosionMixin") && is1_21_2) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
