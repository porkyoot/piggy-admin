package is.pig.minecraft.admin.common;

import is.pig.minecraft.admin.api.IModAdapter;

public class ModCommon {
    private static IModAdapter adapter;

    public static void initialize(IModAdapter modAdapter) {
        adapter = modAdapter;
        System.out.println("piggy-admin: ModCommon initialized with " + modAdapter.getClass().getSimpleName());
    }

    public static IModAdapter getAdapter() {
        return adapter;
    }
}
