package dev.lsdmc.edenCorrections.integrations;

import dev.lsdmc.edenCorrections.EdenCorrections;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class EdenCorrectionsLegacyExpansion extends PlaceholderExpansion {

    private final EdenCorrectionsExpansion delegate;

    public EdenCorrectionsLegacyExpansion(EdenCorrections plugin) {
        this.delegate = new EdenCorrectionsExpansion(plugin);
    }

    @Override
    public String getIdentifier() {
        return "edencorrections";
    }

    @Override
    public String getAuthor() {
        return delegate.getAuthor();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return delegate.onRequest(player, params);
    }
}


