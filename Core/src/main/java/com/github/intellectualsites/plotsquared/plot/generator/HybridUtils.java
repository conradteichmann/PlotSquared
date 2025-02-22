package com.github.intellectualsites.plotsquared.plot.generator;

import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.config.Settings;
import com.github.intellectualsites.plotsquared.plot.flag.FlagManager;
import com.github.intellectualsites.plotsquared.plot.flag.Flags;
import com.github.intellectualsites.plotsquared.plot.listener.WEExtent;
import com.github.intellectualsites.plotsquared.plot.object.*;
import com.github.intellectualsites.plotsquared.plot.util.ChunkManager;
import com.github.intellectualsites.plotsquared.plot.util.MathMan;
import com.github.intellectualsites.plotsquared.plot.util.SchematicHandler;
import com.github.intellectualsites.plotsquared.plot.util.TaskManager;
import com.github.intellectualsites.plotsquared.plot.util.WorldUtil;
import com.github.intellectualsites.plotsquared.plot.util.block.GlobalBlockQueue;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.github.intellectualsites.plotsquared.plot.util.expiry.PlotAnalysis;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class HybridUtils {

    public static HybridUtils manager;
    public static Set<ChunkLoc> regions;
    public static Set<ChunkLoc> chunks = new HashSet<>();
    public static PlotArea area;
    public static boolean UPDATE = false;

    public abstract void analyzeRegion(String world, RegionWrapper region,
        RunnableVal<PlotAnalysis> whenDone);

    public void analyzePlot(final Plot origin, final RunnableVal<PlotAnalysis> whenDone) {
        final ArrayDeque<RegionWrapper> zones = new ArrayDeque<>(origin.getRegions());
        final ArrayList<PlotAnalysis> analysis = new ArrayList<>();
        Runnable run = new Runnable() {
            @Override public void run() {
                if (zones.isEmpty()) {
                    if (!analysis.isEmpty()) {
                        whenDone.value = new PlotAnalysis();
                        for (PlotAnalysis data : analysis) {
                            whenDone.value.air += data.air;
                            whenDone.value.air_sd += data.air_sd;
                            whenDone.value.changes += data.changes;
                            whenDone.value.changes_sd += data.changes_sd;
                            whenDone.value.data += data.data;
                            whenDone.value.data_sd += data.data_sd;
                            whenDone.value.faces += data.faces;
                            whenDone.value.faces_sd += data.faces_sd;
                            whenDone.value.variety += data.variety;
                            whenDone.value.variety_sd += data.variety_sd;
                        }
                        whenDone.value.air /= analysis.size();
                        whenDone.value.air_sd /= analysis.size();
                        whenDone.value.changes /= analysis.size();
                        whenDone.value.changes_sd /= analysis.size();
                        whenDone.value.data /= analysis.size();
                        whenDone.value.data_sd /= analysis.size();
                        whenDone.value.faces /= analysis.size();
                        whenDone.value.faces_sd /= analysis.size();
                        whenDone.value.variety /= analysis.size();
                        whenDone.value.variety_sd /= analysis.size();
                    } else {
                        whenDone.value = analysis.get(0);
                    }
                    List<Integer> result = new ArrayList<>();
                    result.add(whenDone.value.changes);
                    result.add(whenDone.value.faces);
                    result.add(whenDone.value.data);
                    result.add(whenDone.value.air);
                    result.add(whenDone.value.variety);

                    result.add(whenDone.value.changes_sd);
                    result.add(whenDone.value.faces_sd);
                    result.add(whenDone.value.data_sd);
                    result.add(whenDone.value.air_sd);
                    result.add(whenDone.value.variety_sd);
                    FlagManager.addPlotFlag(origin, Flags.ANALYSIS, result);
                    TaskManager.runTask(whenDone);
                    return;
                }
                RegionWrapper region = zones.poll();
                final Runnable task = this;
                analyzeRegion(origin.getWorldName(), region, new RunnableVal<PlotAnalysis>() {
                    @Override public void run(PlotAnalysis value) {
                        analysis.add(value);
                        TaskManager.runTaskLater(task, 1);
                    }
                });
            }
        };
        run.run();
    }

    public int checkModified(LocalBlockQueue queue, int x1, int x2, int y1, int y2, int z1, int z2,
        PlotBlock[] blocks) {
        int count = 0;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    PlotBlock block = queue.getBlock(x, y, z);
                    boolean same =
                        Arrays.stream(blocks).anyMatch(p -> WorldUtil.IMP.isBlockSame(block, p));
                    if (!same) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public final ArrayList<ChunkLoc> getChunks(ChunkLoc region) {
        ArrayList<ChunkLoc> chunks = new ArrayList<>();
        int sx = region.x << 5;
        int sz = region.z << 5;
        for (int x = sx; x < sx + 32; x++) {
            for (int z = sz; z < sz + 32; z++) {
                chunks.add(new ChunkLoc(x, z));
            }
        }
        return chunks;
    }

    public boolean scheduleRoadUpdate(PlotArea area, int extend) {
        if (HybridUtils.UPDATE) {
            return false;
        }
        HybridUtils.UPDATE = true;
        Set<ChunkLoc> regions = ChunkManager.manager.getChunkChunks(area.worldname);
        return scheduleRoadUpdate(area, regions, extend);
    }

    public boolean scheduleSingleRegionRoadUpdate(Plot plot, int extend) {
        if (HybridUtils.UPDATE) {
            return false;
        }
        HybridUtils.UPDATE = true;
        Set<ChunkLoc> regions = new HashSet<>();
        regions.add(ChunkManager.getChunkChunk(plot.getCenter()));
        return scheduleRoadUpdate(plot.getArea(), regions, extend);
    }

    public boolean scheduleRoadUpdate(final PlotArea area, Set<ChunkLoc> rgs, final int extend) {
        HybridUtils.regions = rgs;
        HybridUtils.area = area;
        chunks = new HashSet<>();
        final AtomicInteger count = new AtomicInteger(0);
        TaskManager.runTask(new Runnable() {
            @Override public void run() {
                if (!UPDATE) {
                    Iterator<ChunkLoc> iter = chunks.iterator();
                    while (iter.hasNext()) {
                        ChunkLoc chunk = iter.next();
                        iter.remove();
                        regenerateRoad(area, chunk, extend);
                        ChunkManager.manager.unloadChunk(area.worldname, chunk, true, true);
                    }
                    PlotSquared.debug("&cCancelled road task");
                    return;
                }
                count.incrementAndGet();
                if (count.intValue() % 20 == 0) {
                    PlotSquared.debug("PROGRESS: " + 100 * (2048 - chunks.size()) / 2048 + "%");
                }
                if (regions.isEmpty() && chunks.isEmpty()) {
                    PlotSquared.debug("&3Regenerating plot walls");
                    regeneratePlotWalls(area);

                    HybridUtils.UPDATE = false;
                    PlotSquared.debug(Captions.PREFIX.getTranslated() + "Finished road conversion");
                    // CANCEL TASK
                } else {
                    final Runnable task = this;
                    TaskManager.runTaskAsync(() -> {
                        try {
                            if (chunks.size() < 1024) {
                                if (!regions.isEmpty()) {
                                    Iterator<ChunkLoc> iterator = regions.iterator();
                                    ChunkLoc loc = iterator.next();
                                    iterator.remove();
                                    PlotSquared.debug("&3Updating .mcr: " + loc.x + ", " + loc.z
                                        + " (aprrox 1024 chunks)");
                                    PlotSquared.debug(" - Remaining: " + regions.size());
                                    chunks.addAll(getChunks(loc));
                                    System.gc();
                                }
                            }
                            if (!chunks.isEmpty()) {
                                TaskManager.IMP.sync(new RunnableVal<Object>() {
                                    @Override public void run(Object value) {
                                        long start = System.currentTimeMillis();
                                        Iterator<ChunkLoc> iterator = chunks.iterator();
                                        while (System.currentTimeMillis() - start < 20 && !chunks
                                            .isEmpty()) {
                                            final ChunkLoc chunk = iterator.next();
                                            iterator.remove();
                                            regenerateRoad(area, chunk, extend);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Iterator<ChunkLoc> iterator = regions.iterator();
                            ChunkLoc loc = iterator.next();
                            iterator.remove();
                            PlotSquared.debug(
                                "&c[ERROR]&7 Could not update '" + area.worldname + "/region/r."
                                    + loc.x + "." + loc.z + ".mca' (Corrupt chunk?)");
                            int sx = loc.x << 5;
                            int sz = loc.z << 5;
                            for (int x = sx; x < sx + 32; x++) {
                                for (int z = sz; z < sz + 32; z++) {
                                    ChunkManager.manager
                                        .unloadChunk(area.worldname, new ChunkLoc(x, z), true,
                                            true);
                                }
                            }
                            PlotSquared.debug("&d - Potentially skipping 1024 chunks");
                            PlotSquared.debug("&d - TODO: recommend chunkster if corrupt");
                        }
                        GlobalBlockQueue.IMP.addTask(() -> TaskManager.runTaskLater(task, 20));
                    });
                }
            }
        });
        return true;
    }

    public boolean setupRoadSchematic(Plot plot) {
        final String world = plot.getWorldName();
        final LocalBlockQueue queue = GlobalBlockQueue.IMP.getNewQueue(world, false);
        Location bot = plot.getBottomAbs().subtract(1, 0, 1);
        Location top = plot.getTopAbs();
        final HybridPlotWorld plotworld = (HybridPlotWorld) plot.getArea();
        PlotManager plotManager = plotworld.getPlotManager();
        int sx = bot.getX() - plotworld.ROAD_WIDTH + 1;
        int sz = bot.getZ() + 1;
        int sy = plotworld.ROAD_HEIGHT;
        int ex = bot.getX();
        int ez = top.getZ();
        int ey = get_ey(plotManager, queue, sx, ex, sz, ez, sy);
        int bz = sz - plotworld.ROAD_WIDTH;
        int tz = sz - 1;
        int ty = get_ey(plotManager, queue, sx, ex, bz, tz, sy);

        Set<RegionWrapper> sideRoad =
            new HashSet<>(Collections.singletonList(new RegionWrapper(sx, ex, sy, ey, sz, ez)));
        final Set<RegionWrapper> intersection =
            new HashSet<>(Collections.singletonList(new RegionWrapper(sx, ex, sy, ty, bz, tz)));

        final String dir =
            "schematics" + File.separator + "GEN_ROAD_SCHEMATIC" + File.separator + plot.getArea()
                .toString() + File.separator;

        SchematicHandler.manager.getCompoundTag(world, sideRoad, new RunnableVal<CompoundTag>() {
            @Override public void run(CompoundTag value) {
                SchematicHandler.manager.save(value, dir + "sideroad.schem");
                SchematicHandler.manager
                    .getCompoundTag(world, intersection, new RunnableVal<CompoundTag>() {
                        @Override public void run(CompoundTag value) {
                            SchematicHandler.manager.save(value, dir + "intersection.schem");
                            plotworld.ROAD_SCHEMATIC_ENABLED = true;
                            try {
                                plotworld.setupSchematics();
                            } catch (SchematicHandler.UnsupportedFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            }
        });
        return true;
    }

    public int get_ey(final PlotManager pm, LocalBlockQueue queue, int sx, int ex, int sz, int ez,
        int sy) {
        int ey = sy;
        for (int x = sx; x <= ex; x++) {
            for (int z = sz; z <= ez; z++) {
                for (int y = sy; y <= pm.getWorldHeight(); y++) {
                    if (y > ey) {
                        PlotBlock block = queue.getBlock(x, y, z);
                        if (!block.isAir()) {
                            ey = y;
                        }
                    }
                }
            }
        }
        return ey;
    }

    public boolean regenerateRoad(final PlotArea area, final ChunkLoc chunk, int extend) {
        int x = chunk.x << 4;
        int z = chunk.z << 4;
        int ex = x + 15;
        int ez = z + 15;
        HybridPlotWorld plotWorld = (HybridPlotWorld) area;
        if (!plotWorld.ROAD_SCHEMATIC_ENABLED) {
            return false;
        }
        boolean toCheck = false;
        if (plotWorld.TYPE == 2) {
            boolean c1 = area.contains(x, z);
            boolean c2 = area.contains(ex, ez);
            if (!c1 && !c2) {
                return false;
            } else {
                toCheck = c1 ^ c2;
            }
        }
        PlotManager manager = area.getPlotManager();
        PlotId id1 = manager.getPlotId(x, 0, z);
        PlotId id2 = manager.getPlotId(ex, 0, ez);
        x -= plotWorld.ROAD_OFFSET_X;
        z -= plotWorld.ROAD_OFFSET_Z;
        LocalBlockQueue queue = GlobalBlockQueue.IMP.getNewQueue(plotWorld.worldname, false);
        if (id1 == null || id2 == null || id1 != id2) {
            boolean result = ChunkManager.manager.loadChunk(area.worldname, chunk, false);
            if (result) {
                if (id1 != null) {
                    Plot p1 = area.getPlotAbs(id1);
                    if (p1 != null && p1.hasOwner() && p1.isMerged()) {
                        toCheck = true;
                    }
                }
                if (id2 != null && !toCheck) {
                    Plot p2 = area.getPlotAbs(id2);
                    if (p2 != null && p2.hasOwner() && p2.isMerged()) {
                        toCheck = true;
                    }
                }
                int size = plotWorld.SIZE;
                for (int X = 0; X < 16; X++) {
                    short absX = (short) ((x + X) % size);
                    for (int Z = 0; Z < 16; Z++) {
                        short absZ = (short) ((z + Z) % size);
                        if (absX < 0) {
                            absX += size;
                        }
                        if (absZ < 0) {
                            absZ += size;
                        }
                        boolean condition;
                        if (toCheck) {
                            condition = manager
                                .getPlotId(x + X + plotWorld.ROAD_OFFSET_X, 1,
                                    z + Z + plotWorld.ROAD_OFFSET_Z) == null;
                            //                            condition = MainUtil.isPlotRoad(new Location(plotworld.worldname, x + X, 1, z + Z));
                        } else {
                            boolean gx = absX > plotWorld.PATH_WIDTH_LOWER;
                            boolean gz = absZ > plotWorld.PATH_WIDTH_LOWER;
                            boolean lx = absX < plotWorld.PATH_WIDTH_UPPER;
                            boolean lz = absZ < plotWorld.PATH_WIDTH_UPPER;
                            condition = !gx || !gz || !lx || !lz;
                        }
                        if (condition) {
                            BaseBlock[] blocks = plotWorld.G_SCH.get(MathMan.pair(absX, absZ));
                            int minY = plotWorld.SCHEM_Y;
                            if (!Settings.Schematics.PASTE_ON_TOP)
                                minY = 1;
                            int maxY = Math.max(extend, blocks.length);
                            if (blocks != null) {
                                for (int y = 0; y < maxY; y++) {
                                    if (y > blocks.length - 1) {
                                        queue.setBlock(x + X + plotWorld.ROAD_OFFSET_X, minY + y,
                                            z + Z + plotWorld.ROAD_OFFSET_Z, WEExtent.AIRBASE);
                                    } else {
                                        BaseBlock block = blocks[y];
                                        if (block != null) {
                                            queue
                                                .setBlock(x + X + plotWorld.ROAD_OFFSET_X, minY + y,
                                                    z + Z + plotWorld.ROAD_OFFSET_Z, block);
                                        } else {
                                            queue
                                                .setBlock(x + X + plotWorld.ROAD_OFFSET_X, minY + y,
                                                    z + Z + plotWorld.ROAD_OFFSET_Z,
                                                    WEExtent.AIRBASE);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                queue.enqueue();
                return true;
            }
        }
        return false;
    }

    public boolean regeneratePlotWalls(final PlotArea area) {
        PlotManager plotManager = area.getPlotManager();
        return plotManager.regenerateAllPlotWalls();
    }
}
