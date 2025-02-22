package com.github.intellectualsites.plotsquared.plot.database;

import com.github.intellectualsites.plotsquared.plot.flag.Flag;
import com.github.intellectualsites.plotsquared.plot.object.*;
import com.github.intellectualsites.plotsquared.plot.object.comment.PlotComment;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

public interface AbstractDB {

    /**
     * The UUID that will count as EVERYONE.
     */
    UUID everyone = UUID.fromString("1-1-3-3-7");

    /**
     * Sets Plot owner.
     *
     * @param plot the plot
     * @param uuid the uuid of the new owner
     */
    void setOwner(Plot plot, UUID uuid);

    /**
     * Creates all settings, and create default helpers, trusted + denied lists.
     *
     * @param plots    Plots for which the default table entries should be created
     * @param whenDone the task to run when the method is finished executing
     */
    void createPlotsAndData(List<Plot> plots, Runnable whenDone);

    /**
     * Creates a plot.
     *
     * @param plot the plot to create
     */
    void createPlotSafe(final Plot plot, final Runnable success, final Runnable failure);

    /**
     * Create tables.
     *
     * @throws Exception If the database manager is unable to create the tables
     */
    void createTables() throws Exception;

    /**
     * Deletes a plot.
     *
     * @param plot the plot to delete
     */
    void delete(Plot plot);

    void deleteSettings(Plot plot);

    void deleteHelpers(Plot plot);

    void deleteTrusted(Plot plot);

    /**
     * Removes all denied players from the plot.
     *
     * @param plot the plot
     */
    void deleteDenied(Plot plot);

    /**
     * Deletes all comments from the plot.
     *
     * @param plot the plot
     */
    void deleteComments(Plot plot);

    void deleteRatings(Plot plot);

    void delete(PlotCluster cluster);

    void addPersistentMeta(UUID uuid, String key, byte[] meta, boolean delete);

    void removePersistentMeta(UUID uuid, String key);

    void getPersistentMeta(UUID uuid, RunnableVal<Map<String, byte[]>> result);

    /**
     * Creates the plot settings.
     *
     * @param id   the plot entry id
     * @param plot the plot
     */
    void createPlotSettings(int id, Plot plot);

    /**
     * Gets the table entry ID.
     *
     * @param plot the plot
     * @return {@code Integer} = Plot Entry Id
     */
    int getId(Plot plot);

    /**
     * Gets the id of a given plot cluster.
     *
     * @param cluster PlotCluster Object
     * @return Integer = Cluster Entry Id
     */
    int getClusterId(PlotCluster cluster);

    /**
     * @return A linked HashMap containing all plots
     */
    HashMap<String, HashMap<PlotId, Plot>> getPlots();

    /**
     * @param toValidate
     */
    void validateAllPlots(Set<Plot> toValidate);

    /**
     * @return A HashMap containing all plot clusters
     */
    HashMap<String, Set<PlotCluster>> getClusters();

    /**
     * Sets the merged status for a plot.
     *
     * @param plot   The plot to set the merged status of
     * @param merged boolean[]
     */
    void setMerged(Plot plot, boolean[] merged);

    /**
     * Swaps the settings, helpers etc. of two plots.
     *
     * @param plot1 Plot1
     * @param plot2 Plot2
     */
    void swapPlots(Plot plot1, Plot plot2);

    /**
     * Sets plot flags.
     *
     * @param plot  Plot Object
     * @param flags flags to set
     */
    void setFlags(Plot plot, HashMap<Flag<?>, Object> flags);

    /**
     * Renames a cluster to the given name.
     *
     * @param cluster the cluster to rename
     * @param name    the new cluster name
     */
    void setClusterName(PlotCluster cluster, String name);

    /**
     * Sets the plot alias.
     *
     * @param plot  Plot for which the alias should be set
     * @param alias Plot Alias
     */
    void setAlias(Plot plot, String alias);

    /**
     * Purges a plot.
     *
     * @param uniqueIds list of plot id (db) to be purged
     */
    void purgeIds(Set<Integer> uniqueIds);

    /**
     * Purges a whole world.
     *
     * @param area    World in which the plots should be purged
     * @param plotIds the {@code PlotId}s of {@code Plot}s to purge
     */
    void purge(PlotArea area, Set<PlotId> plotIds);

    /**
     * Sets the plot home position.
     *
     * @param plot     the plot
     * @param position the position of plot home
     */
    void setPosition(Plot plot, String position);

    /**
     * @param cluster
     * @param position
     */
    void setPosition(PlotCluster cluster, String position);

    /**
     * Remove the specified player from the trust list of the specified plot.
     *
     * @param plot the plot
     * @param uuid the uuid of the player to remove
     */
    void removeTrusted(Plot plot, UUID uuid);

    /**
     * @param cluster PlotCluster Object
     * @param uuid    Player that should be removed
     */
    void removeHelper(PlotCluster cluster, UUID uuid);

    /**
     * @param plot the plot
     * @param uuid Player that should be removed
     */
    void removeMember(Plot plot, UUID uuid);

    /**
     * @param cluster
     * @param uuid
     */
    void removeInvited(PlotCluster cluster, UUID uuid);

    /**
     * @param plot Plot Object
     * @param uuid Player that should be removed
     */
    void setTrusted(Plot plot, UUID uuid);

    /**
     * @param cluster PlotCluster Object
     * @param uuid    Player that should be removed
     */
    void setHelper(PlotCluster cluster, UUID uuid);

    /**
     * @param plot Plot Object
     * @param uuid Player that should be added
     */
    void setMember(Plot plot, UUID uuid);

    /**
     * @param cluster
     * @param uuid
     */
    void setInvited(PlotCluster cluster, UUID uuid);

    /**
     * Removes the specified player from the denied list of the specified plot.
     *
     * @param plot the plot
     * @param uuid the uuid of the player to remove
     */
    void removeDenied(Plot plot, UUID uuid);

    /**
     * Denies the specified player from the given plot.
     *
     * @param plot the plot
     * @param uuid the uuid of the player to deny
     */
    void setDenied(Plot plot, UUID uuid);

    /**
     * Gets the ratings from the specified plot.
     *
     * @param plot the plot
     * @return the plot ratings (pre-calculated)
     */
    HashMap<UUID, Integer> getRatings(Plot plot);

    /**
     * Sets a rating for a plot.
     *
     * @param plot
     * @param rater
     * @param value
     */
    void setRating(Plot plot, UUID rater, int value);

    /**
     * Removes the specified comment from the given plot.
     *
     * @param plot    the plot
     * @param comment the comment to remove
     */
    void removeComment(Plot plot, PlotComment comment);

    /**
     * Clears the specified inbox on the given plot.
     *
     * @param plot  the plot
     * @param inbox the inbox to clear
     */
    void clearInbox(Plot plot, String inbox);

    /**
     * Adds the specified comment to the given plot.
     *
     * @param plot    the plot
     * @param comment the comment to add
     */
    void setComment(Plot plot, PlotComment comment);

    /**
     * Gets Plot Comments.
     *
     * @param plot The Plot to get comments from
     */
    void getComments(@NotNull Plot plot, String inbox, RunnableVal<List<PlotComment>> whenDone);

    void createPlotAndSettings(Plot plot, Runnable whenDone);

    void createCluster(PlotCluster cluster);

    void resizeCluster(PlotCluster current, PlotId min, PlotId max);

    void movePlot(Plot originalPlot, Plot newPlot);

    /**
     * Replaces a old uuid with a new one in the database.
     *
     * <ul>
     * <li> Useful for replacing a few uuids (not the entire database).</li>
     * <li>or entire conversion, the uuidconvert command scales better.</li>
     * </ul>
     *
     * @param old
     * @param now
     */
    void replaceUUID(UUID old, UUID now);

    /**
     * Don't use this method unless you want to ruin someone's server.
     *
     * @return true if the tables were deleted, false when an error is encountered
     */
    boolean deleteTables();

    /**
     * Closes the database. Generally not recommended to be used by add-ons.
     */
    void close();

    void replaceWorld(String oldWorld, String newWorld, PlotId min, PlotId max);

    void updateTables(int[] oldVersion);
}
