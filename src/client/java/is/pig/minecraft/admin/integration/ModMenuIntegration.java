package is.pig.minecraft.admin.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import is.pig.minecraft.admin.config.PiggyAdminConfigScreenFactory;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PiggyAdminConfigScreenFactory::create;
    }
}
