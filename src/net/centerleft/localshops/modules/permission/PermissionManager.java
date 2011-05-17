package net.centerleft.localshops.modules.permission;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.centerleft.localshops.LocalShops;
import net.centerleft.localshops.modules.permission.plugins.*;

public class PermissionManager {
    
    private LocalShops plugin = null;
    private TreeMap<Integer,Permission> perms = new TreeMap<Integer,Permission>();
    private Permission activePermission = null;
    private static final Logger log = Logger.getLogger("Minecraft");

    public PermissionManager(LocalShops plugin) {
        this.plugin = plugin;
    }
    
    public boolean load() {

        // Try to load Permissions (Phoenix)
        if (packageExists(new String[] { "com.nijikokun.bukkit.Permissions.Permissions" })) {
            Permission nPerms = new Permission_Permissions(plugin);
            perms.put(9, nPerms);
            log.info(String.format("[%s][Permission] Permissions (Phoenix) found: %s", plugin.getDescription().getName(), nPerms.isEnabled() ? "Loaded" : "Waiting"));
        } else {
            log.info(String.format("[%s][Permission] Permissions (Phoenix) not found.", plugin.getDescription().getName()));
        }
        
        // Try to load Local Fallback Permissions (aka None)
        {
            Permission lPerms = new Permission_None(plugin);
            perms.put(10, lPerms);
            log.info(String.format("[%s][Permission] Local Fallback Permissions found: %s", plugin.getDescription().getName(), lPerms.isEnabled() ? "Loaded" : "Waiting"));
        }
        
        if(perms.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean packageExists(String[] packages) {
        try {
            for (String pkg : packages) {
                Class.forName(pkg);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Permission getPermission() {
        if(activePermission == null) {
            Iterator<Permission> it = perms.values().iterator();
            while(it.hasNext()) {
                Permission p = it.next();
                if(p.isEnabled()) {
                    return p;
                }
            }
            return null;
        } else {
            return activePermission;
        }
    }
    
    public boolean hasPermission(String playerName, String permission) {
        return getPermission().hasPermission(playerName, permission);
    }

}