package net.ME1312.Uno.Network;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;

/**
 * PacketIn Layout Class
 */
public interface PacketIn {
    /**
     * Execute Incoming Packet
     *
     * @param client Client Accepting
     * @param data Incoming Data
     */
    void execute(Client client, ObjectMap<String> data) throws Throwable;

    /**
     * Get Packet Version
     *
     * @return Packet Version
     */
    Version getVersion();

    /**
     * Check Compatibility with oncoming packet
     *
     * @param version Version of oncoming packet
     * @return Compatibility Status
     */
    default boolean isCompatible(Version version) {
        return getVersion().equals(version);
    }
}
